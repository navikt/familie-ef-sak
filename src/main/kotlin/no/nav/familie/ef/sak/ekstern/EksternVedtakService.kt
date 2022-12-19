package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingClient
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.VedtakType
import org.springframework.stereotype.Service

@Service
class EksternVedtakService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilbakekrevingClient: TilbakekrevingClient
) {

    fun hentVedtak(eksternFagsakId: Long): List<FagsystemVedtak> {
        val fagsak = fagsakService.hentFagsakPåEksternId(eksternFagsakId)
        val vedtakTilbakekreving = tilbakekrevingClient.finnVedtak(fagsak.eksternId.id)
        return hentFerdigstilteBehandlinger(fagsak) + vedtakTilbakekreving
    }

    private fun hentFerdigstilteBehandlinger(fagsak: Fagsak): List<FagsystemVedtak> {
        return behandlingService.hentBehandlinger(fagsakId = fagsak.id)
            .filter { it.erAvsluttet() && it.resultat != BehandlingResultat.HENLAGT }
            .map { tilFagsystemVedtak(it) }
    }

    private fun tilFagsystemVedtak(behandling: Behandling): FagsystemVedtak {
        val (resultat, vedtakType) = utledReultatOgVedtakType(behandling)

        return FagsystemVedtak(
            eksternBehandlingId = behandling.eksternId.id.toString(),
            behandlingstype = behandling.type.visningsnavn,
            resultat = resultat,
            vedtakstidspunkt = behandling.vedtakstidspunkt
                ?: error("Mangler vedtakstidspunkt for behandling=${behandling.id}"),
            fagsystemType = FagsystemType.ORDNIÆR,
            vedtakType = vedtakType
        )
    }

    private fun utledReultatOgVedtakType(behandling: Behandling): Pair<String, VedtakType> = when (behandling.årsak) {
        BehandlingÅrsak.SANKSJON_1_MND -> Pair("Sanksjon 1 måned", VedtakType.SANKSJON_1_MND)
        else -> Pair(behandling.resultat.displayName, VedtakType.ORDINÆR)
    }
}
