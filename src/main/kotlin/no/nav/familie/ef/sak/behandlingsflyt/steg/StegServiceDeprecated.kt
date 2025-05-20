package no.nav.familie.ef.sak.behandlingsflyt.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.BEHANDLING_FERDIGSTILT
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.BEREGNE_YTELSE
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.BESLUTTE_VEDTAK
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.FERDIGSTILLE_BEHANDLING
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.LAG_SAKSBEHANDLINGSBLANKETT
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.PUBLISER_VEDTAKSHENDELSE
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.REVURDERING_ÅRSAK
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.SEND_TIL_BESLUTTER
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.vedtak.dto.SendTilBeslutterDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StegServiceDeprecated(
    private val behandlingSteg: List<BehandlingSteg<*>>,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val rolleConfig: RolleConfig,
    private val behandlingshistorikkService: BehandlingshistorikkService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    private val stegSuksessMetrics: Map<StegType, Counter> = initStegMetrikker("suksess")

    private val stegFeiletMetrics: Map<StegType, Counter> = initStegMetrikker("feil")

    @Transactional
    fun håndterBeregnYtelseForStønad(
        saksbehandling: Saksbehandling,
        vedtak: VedtakDto,
    ): Behandling {
        val behandlingSteg: BeregnYtelseSteg = hentBehandlingSteg(BEREGNE_YTELSE)
        return håndterSteg(saksbehandling, behandlingSteg, vedtak)
    }

    @Transactional
    fun håndterSendTilBeslutter(
        saksbehandling: Saksbehandling,
        sendTilBeslutter: SendTilBeslutterDto?,
    ): Behandling {
        val behandlingSteg: SendTilBeslutterSteg = hentBehandlingSteg(SEND_TIL_BESLUTTER)

        return håndterSteg(saksbehandling, behandlingSteg, sendTilBeslutter)
    }

    @Transactional
    fun håndterBeslutteVedtak(
        saksbehandling: Saksbehandling,
        data: BeslutteVedtakDto,
    ): Behandling {
        val behandlingSteg: BeslutteVedtakSteg = hentBehandlingSteg(BESLUTTE_VEDTAK)

        return håndterSteg(saksbehandling, behandlingSteg, data)
    }

    @Transactional
    fun håndterÅrsakRevurdering(
        behandlingId: UUID,
        data: RevurderingsinformasjonDto,
    ): Behandling {
        val årsakRevurderingSteg: ÅrsakRevurderingSteg = hentBehandlingSteg(REVURDERING_ÅRSAK)

        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        return håndterSteg(saksbehandling, årsakRevurderingSteg, data)
    }

    @Transactional
    fun håndterLagSaksbehandlingsblankett(saksbehandling: Saksbehandling): Behandling {
        val behandlingSteg: SaksbehandlingsblankettSteg = hentBehandlingSteg(LAG_SAKSBEHANDLINGSBLANKETT)
        return håndterSteg(saksbehandling, behandlingSteg, null)
    }

    @Transactional
    fun håndterPollStatusFraIverksett(saksbehandling: Saksbehandling): Behandling {
        val behandlingSteg: VentePåStatusFraIverksett = hentBehandlingSteg(VENTE_PÅ_STATUS_FRA_IVERKSETT)

        return håndterSteg(saksbehandling, behandlingSteg, null)
    }

    @Transactional
    fun publiserVedtakshendelse(behandlingId: UUID): Behandling {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val behandlingSteg: PubliserVedtakshendelseSteg = hentBehandlingSteg(PUBLISER_VEDTAKSHENDELSE)

        return håndterSteg(behandling, behandlingSteg, null)
    }

    @Transactional
    fun håndterFerdigstillBehandling(saksbehandling: Saksbehandling): Behandling {
        val behandlingSteg: FerdigstillBehandlingSteg = hentBehandlingSteg(FERDIGSTILLE_BEHANDLING)

        return håndterSteg(saksbehandling, behandlingSteg, null)
    }

    @Transactional
    fun resetSteg(
        behandlingId: UUID,
        steg: StegType,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.status != BehandlingStatus.UTREDES) {
            error("Kan ikke endre steg når status=${behandling.status} behandling=$behandlingId")
        }
        if (steg.kommerEtter(behandling.steg)) {
            error(
                "Kan ikke sette behandling til steg=$steg når behandling allerede " +
                    "er på ${behandling.steg} behandling=$behandlingId",
            )
        }

        validerAtStegKanResettes(behandling, steg)
        behandlingService.oppdaterStegPåBehandling(behandlingId, steg)
    }

    @Transactional
    fun angreSendTilBeslutter(behandlingId: UUID) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val beslutter = vedtakService.hentVedtak(behandlingId).beslutterIdent

        feilHvis(saksbehandling.steg != BESLUTTE_VEDTAK, httpStatus = HttpStatus.BAD_REQUEST) {
            if (saksbehandling.steg.kommerEtter(BESLUTTE_VEDTAK)) {
                "Kan ikke angre send til beslutter da vedtaket er godkjent av $beslutter"
            } else {
                "Kan ikke angre send til beslutter når behandling er i steg ${saksbehandling.steg}"
            }
        }

        feilHvis(saksbehandling.status != BehandlingStatus.FATTER_VEDTAK, httpStatus = HttpStatus.BAD_REQUEST) {
            "Kan ikke angre send til beslutter når behandlingen har status ${saksbehandling.status}"
        }

        behandlingService.oppdaterStegPåBehandling(behandlingId, SEND_TIL_BESLUTTER)
        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES)
    }

    private fun validerAtStegKanResettes(
        behandling: Behandling,
        steg: StegType,
    ) {
        val harTilgangTilSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, behandling.steg.tillattFor)
        val harTilgangTilNesteSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, steg.tillattFor)
        if (!harTilgangTilSteg || !harTilgangTilNesteSteg) {
            val saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
            error(
                "$saksbehandler kan ikke endre" +
                    " fra steg=${behandling.steg.displayName()} til steg=${steg.displayName()}" +
                    " pga manglende rolle på behandling=$behandling.id",
            )
        }
    }

    // Generelle stegmetoder
    private fun <T> håndterSteg(
        saksbehandling: Saksbehandling,
        behandlingSteg: BehandlingSteg<T>,
        data: T,
    ): Behandling {
        val stegType = behandlingSteg.stegType()
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        try {
            valider(saksbehandling, stegType, saksbehandlerIdent, behandlingSteg)
            val nesteSteg = behandlingSteg.utførOgReturnerNesteSteg(saksbehandling, data)
            oppdaterHistorikk(behandlingSteg, saksbehandling.id, saksbehandlerIdent)
            oppdaterMetrikk(stegType, stegSuksessMetrics)
            validerNesteSteg(nesteSteg, saksbehandling)
            logger.info("$stegType på behandling ${saksbehandling.id} er håndtert")
            return behandlingService.oppdaterStegPåBehandling(behandlingId = saksbehandling.id, steg = nesteSteg)
        } catch (exception: Exception) {
            oppdaterMetrikk(stegType, stegFeiletMetrics)
            logger.warn("Håndtering av stegtype '$stegType' feilet på behandling ${saksbehandling.id}.")
            throw exception
        }
    }

    private fun validerNesteSteg(
        nesteSteg: StegType,
        saksbehandling: Saksbehandling,
    ) {
        if (!nesteSteg.erGyldigIKombinasjonMedStatus(behandlingService.hentBehandling(saksbehandling.id).status)) {
            error(
                "Steg '${nesteSteg.displayName()}' kan ikke settes " +
                    "på behandling i kombinasjon med status ${saksbehandling.status}",
            )
        }
    }

    private fun <T> valider(
        saksbehandling: Saksbehandling,
        stegType: StegType,
        saksbehandlerIdent: String,
        behandlingSteg: BehandlingSteg<T>,
    ) {
        utførBehandlingsvalidering(behandlingSteg, saksbehandling)
        validerHarTilgang(saksbehandling, stegType, saksbehandlerIdent)
        validerGyldigTilstand(saksbehandling, stegType, saksbehandlerIdent)
    }

    private fun oppdaterMetrikk(
        stegType: StegType,
        metrikk: Map<StegType, Counter>,
    ) {
        metrikk[stegType]?.increment()
    }

    private fun <T> oppdaterHistorikk(
        behandlingSteg: BehandlingSteg<T>,
        behandlingId: UUID,
        saksbehandlerIdent: String,
    ) {
        if (behandlingSteg.settInnHistorikk()) {
            behandlingshistorikkService.opprettHistorikkInnslag(
                Behandlingshistorikk(
                    behandlingId = behandlingId,
                    steg = behandlingSteg.stegType(),
                    opprettetAvNavn = SikkerhetContext.hentSaksbehandlerNavn(),
                    opprettetAv = saksbehandlerIdent,
                ),
            )
        }
    }

    private fun <T> utførBehandlingsvalidering(
        behandlingSteg: BehandlingSteg<T>,
        saksbehandling: Saksbehandling,
    ) {
        behandlingSteg.validerSteg(saksbehandling)
        feilHvis(!behandlingSteg.stegType().erGyldigIKombinasjonMedStatus(saksbehandling.status)) {
            "Kan ikke utføre '${
                behandlingSteg.stegType().displayName()
            }' når behandlingstatus er ${saksbehandling.status.visningsnavn()}"
        }
    }

    private fun validerGyldigTilstand(
        saksbehandling: Saksbehandling,
        stegType: StegType,
        saksbehandlerIdent: String,
    ) {
        if (saksbehandling.steg == BEHANDLING_FERDIGSTILT) {
            error("Behandlingen er avsluttet og stegprosessen kan ikke gjenåpnes")
        }

        if (stegType.kommerEtter(saksbehandling.steg)) {
            error(
                "$saksbehandlerIdent prøver å utføre steg '${stegType.displayName()}', " +
                    "men behandlingen er på steg '${saksbehandling.steg.displayName()}'",
            )
        }

        if (saksbehandling.steg == BESLUTTE_VEDTAK && stegType != BESLUTTE_VEDTAK) {
            error("Behandlingen er på steg '${saksbehandling.steg.displayName()}', og er da låst for alle andre type endringer.")
        }
    }

    private fun validerHarTilgang(
        saksbehandling: Saksbehandling,
        stegType: StegType,
        saksbehandlerIdent: String,
    ) {
        val rolleForSteg: BehandlerRolle = utledRolleForSteg(stegType, saksbehandling)
        val harTilgangTilSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, rolleForSteg)

        logger.info("Starter håndtering av $stegType på behandling ${saksbehandling.id}")
        secureLogger.info(
            "Starter håndtering av $stegType på behandling " +
                "${saksbehandling.id} med saksbehandler=[$saksbehandlerIdent]",
        )

        feilHvis(!harTilgangTilSteg) {
            "$saksbehandlerIdent kan ikke utføre steg '${stegType.displayName()}' pga manglende rolle."
        }
    }

    private fun utledRolleForSteg(
        stegType: StegType,
        saksbehandling: Saksbehandling,
    ): BehandlerRolle {
        if (stegType == BESLUTTE_VEDTAK) {
            val vedtak = vedtakService.hentVedtak(saksbehandling.id)
            if (vedtak.erVedtakUtenBeslutter()) {
                return BehandlerRolle.SAKSBEHANDLER
            }
        }
        return saksbehandling.steg.tillattFor
    }

    private fun <T : BehandlingSteg<*>> hentBehandlingSteg(stegType: StegType): T {
        val firstOrNull =
            behandlingSteg.singleOrNull { it.stegType() == stegType }
                ?: error("Finner ikke behandling steg for type $stegType")
        @Suppress("UNCHECKED_CAST")
        return firstOrNull as T
    }

    private fun initStegMetrikker(type: String): Map<StegType, Counter> =
        behandlingSteg.associate {
            it.stegType() to
                Metrics.counter(
                    "behandling.steg.$type",
                    "steg",
                    it.stegType().name,
                    "beskrivelse",
                    "${it.stegType().rekkefølge} ${it.stegType().displayName()}",
                )
        }
}
