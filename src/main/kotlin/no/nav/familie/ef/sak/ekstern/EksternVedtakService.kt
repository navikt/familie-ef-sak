package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import org.springframework.stereotype.Service

@Service
class EksternVedtakService(
    val tilkjentYtelseService: TilkjentYtelseService,
    val behandlingService: BehandlingService,
    val fagsakService: FagsakService,
    val infotrygdService: InfotrygdService
) {

    fun hentVedtak(eksternFagsakId: Long): List<FagsystemVedtak> {
        val fagsak = fagsakService.hentFagsakPåEksternId(eksternFagsakId)
        val ferdigstilteBehandlinger =
            behandlingService.hentBehandlinger(fagsakId = fagsak.id).filter { it.erAvsluttet() }

        return ferdigstilteBehandlinger.map { tilFagsystemVedtak(it) }
    }

    private fun tilFagsystemVedtak(behandling: Behandling) = FagsystemVedtak(
        eksternBehandlingId = behandling.eksternId.id.toString(),
        behandlingstype = behandling.type.visningsnavn,
        resultat = behandling.resultat.displayName,
        vedtakstidspunkt = if (behandling.status == BehandlingStatus.FERDIGSTILT) {
            behandling.sporbar.endret.endretTid
        } else {
            throw Feil(
                "Kan ikke utlede vedtaksdato for behandling"
            )
        }
    )
}
