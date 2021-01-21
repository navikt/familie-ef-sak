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
    fun lagreTotrinnskontroll(behandling: Behandling, totrinnskontrollDto: TotrinnskontrollDto): String {
        val sisteBehandlingshistorikk = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = behandling.id)

        if (sisteBehandlingshistorikk.steg != StegType.BESLUTTE_VEDTAK) {
            throw Feil("Siste innslag i behandlingshistorikken har feil steg=${sisteBehandlingshistorikk.steg}")
        }

        if (beslutterErLikBehandler(sisteBehandlingshistorikk)) {
            throw Feil(message = "Beslutter er lik behandler",
                       frontendFeilmelding = "Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter")
        }

        lagreTotrinnskontrollIHistorikk(behandling, totrinnskontrollDto)

        val nyStatus = if (totrinnskontrollDto.godkjent) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.UTREDES
        behandlingService.oppdaterStatusPåBehandling(behandling.id, nyStatus)
        return sisteBehandlingshistorikk.opprettetAv
    }

    private fun lagreTotrinnskontrollIHistorikk(behandling: Behandling,
                                                totrinnskontrollDto: TotrinnskontrollDto) {
        behandlingshistorikkService.opprettHistorikkInnslag(
                Behandlingshistorikk(
                        behandlingId = behandling.id,
                        steg = behandling.steg,
                        utfall = if (totrinnskontrollDto.godkjent) BESLUTTE_VEDTAK_GODKJENT else BESLUTTE_VEDTAK_UNDERKJENT,
                        metadata = objectMapper.writeValueAsString(totrinnskontrollDto)))
    }

    fun hentTotrinnskontroll(behandlingId: UUID): TotrinnskontrollStatusDto {
        val behandlingStatus = behandlingService.hentBehandling(behandlingId).status

        if (behandlingStatus == BehandlingStatus.FERDIGSTILT || behandlingStatus == BehandlingStatus.IVERKSETTER_VEDTAK) {
            return TotrinnskontrollStatusDto(UAKTUELT)
        }
        val beslutteVedtakHendelse = sisteBeslutteVedtakHendelse(behandlingId)
        return when {
            behandlingStatus == BehandlingStatus.FATTER_VEDTAK -> behandlingStatusFatterVedtak(beslutteVedtakHendelse)
            beslutteVedtakHendelse != null -> harBesluttetVedtak(beslutteVedtakHendelse)
            else -> TotrinnskontrollStatusDto(UAKTUELT)
        }
    }

    private fun behandlingStatusFatterVedtak(beslutteVedtakHendelse: Behandlingshistorikk?): TotrinnskontrollStatusDto {
        requireNotNull(beslutteVedtakHendelse) { "BehandlingStatus=${BehandlingStatus.FATTER_VEDTAK} - mangler historikk" }
        return if (beslutterErLikBehandler(beslutteVedtakHendelse)) {
            TotrinnskontrollStatusDto(IKKE_AUTORISERT)
        } else {
            TotrinnskontrollStatusDto(KAN_FATTE_VEDTAK)
        }
    }

    private fun beslutterErLikBehandler(beslutteVedtakHendelse: Behandlingshistorikk) =
            SikkerhetContext.hentSaksbehandler() == beslutteVedtakHendelse.opprettetAv

    private fun harBesluttetVedtak(beslutteVedtakHendelse: Behandlingshistorikk): TotrinnskontrollStatusDto {
        return when (beslutteVedtakHendelse.utfall) {
            BESLUTTE_VEDTAK_UNDERKJENT -> {
                requireNotNull(beslutteVedtakHendelse.metadata) { "Har underkjent vedtak - savner metadata" }
                val totrinnskontroll = objectMapper.readValue<TotrinnskontrollDto>(beslutteVedtakHendelse.metadata)
                TotrinnskontrollStatusDto(TOTRINNSKONTROLL_UNDERKJENT, totrinnskontroll.begrunnelse)
            }
            else -> error("Skal ikke kunne være annen status enn UNDERKJENT når behandligStatus!=${BehandlingStatus.FATTER_VEDTAK}")
        }
    }

    private fun sisteBeslutteVedtakHendelse(behandlingId: UUID): Behandlingshistorikk? =
            behandlingshistorikkService.finnBehandlingshistorikk(behandlingId)
                    .filter { it.steg == StegType.BESLUTTE_VEDTAK }
                    .maxByOrNull { it.endretTid }

}