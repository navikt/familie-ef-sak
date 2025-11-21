package no.nav.familie.ef.sak.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall.ANGRE_SEND_TIL_BESLUTTER
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall.BESLUTTE_VEDTAK_GODKJENT
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT
import no.nav.familie.ef.sak.beregning.ValiderOmregningService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext.NAVIDENT_REGEX
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.TotrinnkontrollStatus.IKKE_AUTORISERT
import no.nav.familie.ef.sak.vedtak.dto.TotrinnkontrollStatus.KAN_FATTE_VEDTAK
import no.nav.familie.ef.sak.vedtak.dto.TotrinnkontrollStatus.TOTRINNSKONTROLL_UNDERKJENT
import no.nav.familie.ef.sak.vedtak.dto.TotrinnkontrollStatus.UAKTUELT
import no.nav.familie.ef.sak.vedtak.dto.TotrinnskontrollDto
import no.nav.familie.ef.sak.vedtak.dto.TotrinnskontrollStatusDto
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TotrinnskontrollService(
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
    private val validerOmregningService: ValiderOmregningService,
) {
    /**
     * Lagrer data om besluttning av totrinnskontroll
     * og returnerer navIdent til saksbehandleren som sendte behandling til beslutter
     */
    @Transactional
    fun lagreTotrinnskontrollOgReturnerBehandler(
        saksbehandling: Saksbehandling,
        beslutteVedtak: BeslutteVedtakDto,
        vedtakErUtenBeslutter: VedtakErUtenBeslutter,
    ): String {
        val sisteBehandlingshistorikk =
            behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId = saksbehandling.id)
        validerOmregningService.validerHarGammelGOgKanLagres(saksbehandling)
        if (sisteBehandlingshistorikk.steg != StegType.SEND_TIL_BESLUTTER) {
            throw Feil(
                message = "Siste innslag i behandlingshistorikken har feil steg=${sisteBehandlingshistorikk.steg}",
                frontendFeilmelding = "Behandlingen er i feil steg, last siden på nytt",
            )
        }

        if (!vedtakErUtenBeslutter.value && beslutterErLikBehandler(sisteBehandlingshistorikk)) {
            throw ApiFeil(
                "Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter",
                HttpStatus.BAD_REQUEST,
            )
        }

        val nyStatus = if (beslutteVedtak.godkjent) BehandlingStatus.IVERKSETTER_VEDTAK else BehandlingStatus.UTREDES
        val utfall = if (beslutteVedtak.godkjent) BESLUTTE_VEDTAK_GODKJENT else BESLUTTE_VEDTAK_UNDERKJENT

        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = saksbehandling.id,
            stegtype = saksbehandling.steg,
            utfall = utfall,
            metadata = beslutteVedtak,
        )

        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, nyStatus)
        return sisteBehandlingshistorikk.opprettetAv
    }

    fun hentSaksbehandlerSomSendteTilBeslutter(behandlingId: UUID): String {
        val sisteBehandlingshistorikk =
            behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId, StegType.SEND_TIL_BESLUTTER)

        return sisteBehandlingshistorikk?.opprettetAv ?: throw Feil("Fant ikke saksbehandler som sendte til beslutter")
    }

    fun hentBeslutter(behandlingId: UUID): String? =
        behandlingshistorikkService
            .finnSisteBehandlingshistorikk(behandlingId, StegType.BESLUTTE_VEDTAK)
            ?.opprettetAv
            ?.takeIf { NAVIDENT_REGEX.matches(it) }

    fun hentTotrinnskontrollStatus(behandlingId: UUID): TotrinnskontrollStatusDto {
        val behandlingStatus = behandlingService.hentBehandling(behandlingId).status

        if (behandlingErGodkjentEllerOpprettet(behandlingStatus)) {
            return TotrinnskontrollStatusDto(UAKTUELT)
        }

        return when (behandlingStatus) {
            BehandlingStatus.FATTER_VEDTAK -> finnStatusForVedtakSomSkalFattes(behandlingId)
            BehandlingStatus.UTREDES -> finnStatusForVedtakSomErFattet(behandlingId)
            else -> error("Har ikke lagt til håndtering av behandlingStatus=$behandlingStatus")
        }
    }

    private fun behandlingErGodkjentEllerOpprettet(behandlingStatus: BehandlingStatus) =
        behandlingStatus == BehandlingStatus.FERDIGSTILT ||
            behandlingStatus == BehandlingStatus.IVERKSETTER_VEDTAK ||
            behandlingStatus == BehandlingStatus.SATT_PÅ_VENT ||
            behandlingStatus == BehandlingStatus.OPPRETTET

    /**
     * Hvis behandlingsstatus er FATTER_VEDTAK så sjekkes det att saksbehandleren er autorisert til å fatte vedtak
     */
    private fun finnStatusForVedtakSomSkalFattes(behandlingId: UUID): TotrinnskontrollStatusDto {
        val historikkHendelse = behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId)
        if (historikkHendelse.steg != StegType.SEND_TIL_BESLUTTER) {
            throw Feil(
                message = "Siste historikken har feil steg, steg=${historikkHendelse.steg}",
                frontendFeilmelding = "Feil i historikken, kontakt brukerstøtte id=$behandlingId",
            )
        }
        return if (beslutterErLikBehandler(historikkHendelse) || !tilgangService.harTilgangTilRolle(BehandlerRolle.BESLUTTER)) {
            TotrinnskontrollStatusDto(
                IKKE_AUTORISERT,
                TotrinnskontrollDto(historikkHendelse.opprettetAvNavn, historikkHendelse.endretTid),
            )
        } else {
            TotrinnskontrollStatusDto(KAN_FATTE_VEDTAK)
        }
    }

    /**
     * Hvis behandlingen utredes sjekkes det for om det finnes ett tidligere beslutt, som då kun kan være underkjent
     */
    private fun finnStatusForVedtakSomErFattet(behandlingId: UUID): TotrinnskontrollStatusDto {
        val besluttetVedtakHendelse =
            behandlingshistorikkService.finnSisteBehandlingshistorikk(behandlingId, StegType.BESLUTTE_VEDTAK)
                ?: return TotrinnskontrollStatusDto(UAKTUELT)
        return when (besluttetVedtakHendelse.utfall) {
            BESLUTTE_VEDTAK_UNDERKJENT -> {
                if (besluttetVedtakHendelse.metadata == null) {
                    throw Feil(
                        message = "Har underkjent vedtak - savner metadata",
                        frontendFeilmelding = "Savner metadata, kontakt brukerstøtte id=$behandlingId",
                    )
                }
                val beslutt = objectMapper.readValue<BeslutteVedtakDto>(besluttetVedtakHendelse.metadata.json)
                TotrinnskontrollStatusDto(
                    TOTRINNSKONTROLL_UNDERKJENT,
                    TotrinnskontrollDto(
                        besluttetVedtakHendelse.opprettetAvNavn,
                        besluttetVedtakHendelse.endretTid,
                        beslutt.godkjent,
                        beslutt.begrunnelse,
                        beslutt.årsakerUnderkjent,
                    ),
                )
            }

            ANGRE_SEND_TIL_BESLUTTER -> {
                TotrinnskontrollStatusDto(UAKTUELT)
            }

            else -> {
                error(
                    "Skal ikke kunne være annen status enn UNDERKJENT når " +
                        "behandligStatus!=${BehandlingStatus.FATTER_VEDTAK}",
                )
            }
        }
    }

    private fun beslutterErLikBehandler(beslutteVedtakHendelse: Behandlingshistorikk) = SikkerhetContext.hentSaksbehandlerEllerSystembruker() == beslutteVedtakHendelse.opprettetAv
}
