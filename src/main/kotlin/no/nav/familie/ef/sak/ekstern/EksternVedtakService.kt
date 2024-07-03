package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingClient
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.BehandlingKategori
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import org.springframework.stereotype.Service

@Service
class EksternVedtakService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilbakekrevingClient: TilbakekrevingClient,
) {
    fun hentVedtak(eksternFagsakId: Long): List<FagsystemVedtak> {
        val fagsak = fagsakService.hentFagsakPåEksternId(eksternFagsakId)
        val vedtakTilbakekreving = tilbakekrevingClient.finnVedtak(fagsak.eksternId)
        return hentFerdigstilteBehandlinger(fagsak) + vedtakTilbakekreving
    }

    private fun hentFerdigstilteBehandlinger(fagsak: Fagsak): List<FagsystemVedtak> =
        behandlingService
            .hentBehandlinger(fagsakId = fagsak.id)
            .filter { it.erAvsluttet() && it.resultat != BehandlingResultat.HENLAGT }
            .map { tilFagsystemVedtak(it) }

    private fun tilFagsystemVedtak(behandling: Behandling): FagsystemVedtak {
        val (resultat, fagsystemType) = utledReultatOgFagsystemType(behandling)

        return FagsystemVedtak(
            eksternBehandlingId = behandling.eksternId.toString(),
            behandlingstype = behandling.type.visningsnavn,
            resultat = resultat,
            vedtakstidspunkt =
                behandling.vedtakstidspunkt
                    ?: error("Mangler vedtakstidspunkt for behandling=${behandling.id}"),
            fagsystemType = fagsystemType,
            regelverk = mapTilRegelverk(behandling.kategori),
        )
    }

    private fun utledReultatOgFagsystemType(behandling: Behandling): Pair<String, FagsystemType> =
        when (behandling.årsak) {
            BehandlingÅrsak.SANKSJON_1_MND -> Pair("Sanksjon 1 måned", FagsystemType.SANKSJON_1_MND)
            else -> Pair(behandling.resultat.displayName, FagsystemType.ORDNIÆR)
        }

    private fun mapTilRegelverk(kategori: BehandlingKategori) =
        when (kategori) {
            BehandlingKategori.EØS -> Regelverk.EØS
            BehandlingKategori.NASJONAL -> Regelverk.NASJONAL
        }
}
