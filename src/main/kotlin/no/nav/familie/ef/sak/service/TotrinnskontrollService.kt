package no.nav.familie.ef.sak.service

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.TotrinnkontrollStatus.*
import no.nav.familie.ef.sak.api.dto.TotrinnskontrollDto
import no.nav.familie.ef.sak.api.dto.TotrinnskontrollStatusDto
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.repository.domain.StegUtfall.BESLUTTE_VEDTAK_GODKJENT
import no.nav.familie.ef.sak.repository.domain.StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TotrinnskontrollService(private val behandlingshistorikkService: BehandlingshistorikkService,
                              private val behandlingService: BehandlingService) {

    /**
     * @return ident til saksbehandler som godkjente vedtaket
     */
    @Transactional
    fun lagreTotrinnskontroll(behandling: Behandling, totrinnskontrollDto: TotrinnskontrollDto) {
        val sisteBehandlingshistorikk = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = behandling.id)

        require(sisteBehandlingshistorikk.steg == StegType.BESLUTTE_VEDTAK) {
            "Siste innslag i behandlingshistorikken har feil steg=${sisteBehandlingshistorikk.steg}"
        }

        if (beslutterErLikBehandler(sisteBehandlingshistorikk)) {
            throw Feil(message = "Beslutter er lik behandler",
                       frontendFeilmelding = "Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter")
        }

        val nyStatus = if (totrinnskontrollDto.godkjent) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.UTREDES
        val utfall = if (totrinnskontrollDto.godkjent) BESLUTTE_VEDTAK_GODKJENT else BESLUTTE_VEDTAK_UNDERKJENT

        behandlingshistorikkService.opprettHistorikkInnslag(behandling = behandling,
                                                            utfall = utfall,
                                                            metadata = totrinnskontrollDto)

        behandlingService.oppdaterStatusPåBehandling(behandling.id, nyStatus)
    }

    fun hentTotrinnskontrollStatus(behandlingId: UUID): TotrinnskontrollStatusDto {
        val behandlingStatus = behandlingService.hentBehandling(behandlingId).status

        if (behandlingErGodkjendEllerOpprettet(behandlingStatus)) {
            return TotrinnskontrollStatusDto(UAKTUELT)
        }

        val beslutteVedtakHendelse = sisteBeslutteVedtakHendelse(behandlingId) ?: return TotrinnskontrollStatusDto(UAKTUELT)

        return when (behandlingStatus) {
            BehandlingStatus.FATTER_VEDTAK -> skalFatteVedtak(beslutteVedtakHendelse)
            BehandlingStatus.UTREDES -> harBesluttetVedtak(beslutteVedtakHendelse)
            else -> error("Har ikke lagt til håndtering av behandlingStatus=$behandlingStatus")
        }
    }

    private fun behandlingErGodkjendEllerOpprettet(behandlingStatus: BehandlingStatus) =
            behandlingStatus == BehandlingStatus.FERDIGSTILT
            || behandlingStatus == BehandlingStatus.IVERKSETTER_VEDTAK
            || behandlingStatus == BehandlingStatus.OPPRETTET

    private fun skalFatteVedtak(beslutteVedtakHendelse: Behandlingshistorikk): TotrinnskontrollStatusDto {
        return if (beslutterErLikBehandler(beslutteVedtakHendelse)) {
            TotrinnskontrollStatusDto(IKKE_AUTORISERT)
        } else {
            TotrinnskontrollStatusDto(KAN_FATTE_VEDTAK)
        }
    }

    private fun harBesluttetVedtak(beslutteVedtakHendelse: Behandlingshistorikk): TotrinnskontrollStatusDto {
        return when (beslutteVedtakHendelse.utfall) {
            BESLUTTE_VEDTAK_UNDERKJENT -> {
                requireNotNull(beslutteVedtakHendelse.metadata) { "Har underkjent vedtak - savner metadata" }
                val totrinnskontroll = objectMapper.readValue<TotrinnskontrollDto>(beslutteVedtakHendelse.metadata.json)
                TotrinnskontrollStatusDto(TOTRINNSKONTROLL_UNDERKJENT, totrinnskontroll.begrunnelse)
            }
            else -> error("Skal ikke kunne være annen status enn UNDERKJENT når behandligStatus!=${BehandlingStatus.FATTER_VEDTAK}")
        }
    }

    private fun sisteBeslutteVedtakHendelse(behandlingId: UUID) =
            behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId, StegType.BESLUTTE_VEDTAK)

    private fun beslutterErLikBehandler(beslutteVedtakHendelse: Behandlingshistorikk) =
            SikkerhetContext.hentSaksbehandler() == beslutteVedtakHendelse.opprettetAv
}
