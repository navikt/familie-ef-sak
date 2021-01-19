package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.TotrinnskontrollDto
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.service.steg.StegType
import org.springframework.stereotype.Service

@Service
class TotrinnskontrollService(private val behandlingshistorikkService: BehandlingshistorikkService,
                              private val behandlingService: BehandlingService) {

    /**
     * @return ident til saksbehandler som godkjente vedtaket
     */
    fun lagreTotrinnskontroll(behandling: Behandling, totrinnskontrollDto: TotrinnskontrollDto): String {
        val sisteBehandlingshistorikk =
                behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = behandling.id)

        if (sisteBehandlingshistorikk.steg != StegType.SEND_TIL_BESLUTTER) {
            throw Feil("Siste innslag i behandlingshistorikken har feil steg=${sisteBehandlingshistorikk.steg}")
        }

        /*behandlingshistorikkService.opprettHistorikkInnslag(
                Behandlingshistorikk(behandlingId = behandling.id,
                                     steg = behandling.steg,
                                     utfall = ,
                                     metadata = objectMapper,

                ))*/
        val nyStatus = if (totrinnskontrollDto.godkjent) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.UTREDES
        behandlingService.oppdaterStatusPåBehandling(behandling.id, nyStatus)
        return sisteBehandlingshistorikk.opprettetAv
    }
}