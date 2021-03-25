package no.nav.familie.ef.sak.service.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ef.sak.api.beregning.VedtakDto
import no.nav.familie.ef.sak.api.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.blankett.BlankettSteg
import no.nav.familie.ef.sak.config.RolleConfig
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.BehandlingshistorikkService
import no.nav.familie.ef.sak.service.steg.StegType.BEHANDLING_FERDIGSTILT
import no.nav.familie.ef.sak.service.steg.StegType.BEREGNE_YTELSE
import no.nav.familie.ef.sak.service.steg.StegType.BESLUTTE_VEDTAK
import no.nav.familie.ef.sak.service.steg.StegType.DISTRIBUER_VEDTAKSBREV
import no.nav.familie.ef.sak.service.steg.StegType.FERDIGSTILLE_BEHANDLING
import no.nav.familie.ef.sak.service.steg.StegType.IVERKSETT_MOT_OPPDRAG
import no.nav.familie.ef.sak.service.steg.StegType.JOURNALFØR_BLANKETT
import no.nav.familie.ef.sak.service.steg.StegType.JOURNALFØR_VEDTAKSBREV
import no.nav.familie.ef.sak.service.steg.StegType.SEND_TIL_BESLUTTER
import no.nav.familie.ef.sak.service.steg.StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI
import no.nav.familie.ef.sak.service.steg.StegType.VEDTA_BLANKETT
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StegService(private val behandlingSteg: List<BehandlingSteg<*>>,
                  private val behandlingService: BehandlingService,
                  private val rolleConfig: RolleConfig,
                  private val behandlingshistorikkService: BehandlingshistorikkService) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")


    private val stegSuksessMetrics: Map<StegType, Counter> = initStegMetrikker("suksess")

    private val stegFeiletMetrics: Map<StegType, Counter> = initStegMetrikker("feil")

    @Transactional
    fun håndterVilkår(behandling: Behandling): Behandling {
        val behandlingSteg: VilkårSteg = hentBehandlingSteg(StegType.VILKÅR)
        return håndterSteg(behandling, behandlingSteg, null)
    }

    @Transactional
    fun håndterBeregnYtelseForStønad(behandling: Behandling, tilkjentYtelse: TilkjentYtelseDTO): Behandling {
        val behandlingSteg: BeregnYtelseSteg = hentBehandlingSteg(BEREGNE_YTELSE)
        return håndterSteg(behandling, behandlingSteg, tilkjentYtelse)
    }

    @Transactional
    fun håndterVedtaBlankett(behandling: Behandling, vedtak: VedtakDto): Behandling {
        val behandlingSteg: VedtaBlankettSteg = hentBehandlingSteg(VEDTA_BLANKETT)
        return håndterSteg(behandling, behandlingSteg, vedtak)
    }

    @Transactional
    fun håndterSendTilBeslutter(behandling: Behandling): Behandling {
        val behandlingSteg: SendTilBeslutterSteg = hentBehandlingSteg(SEND_TIL_BESLUTTER)

        return håndterSteg(behandling, behandlingSteg, null)
    }

    @Transactional
    fun håndterBeslutteVedtak(behandling: Behandling, data: BeslutteVedtakDto): Behandling {
        val behandlingSteg: BeslutteVedtakSteg = hentBehandlingSteg(BESLUTTE_VEDTAK)

        return håndterSteg(behandling, behandlingSteg, data)
    }

    @Transactional
    fun håndterBlankett(behandling: Behandling): Behandling {
        val behandlingSteg: BlankettSteg = hentBehandlingSteg(JOURNALFØR_BLANKETT)

        return håndterSteg(behandling, behandlingSteg, null)
    }

    @Transactional
    fun håndterIverksettingOppdrag(behandling: Behandling): Behandling {
        val behandlingSteg: IverksettMotOppdragSteg = hentBehandlingSteg(IVERKSETT_MOT_OPPDRAG)

        return håndterSteg(behandling, behandlingSteg, null)
    }

    @Transactional
    fun håndterStatusPåOppdrag(behandling: Behandling): Behandling {
        val behandlingSteg: VentePåStatusFraØkonomi = hentBehandlingSteg(VENTE_PÅ_STATUS_FRA_ØKONOMI)

        return håndterSteg(behandling, behandlingSteg, null)
    }

    @Transactional
    fun håndterJournalførVedtaksbrev(behandling: Behandling): Behandling {
        val behandlingSteg: JournalførVedtaksbrevSteg = hentBehandlingSteg(JOURNALFØR_VEDTAKSBREV)

        return håndterSteg(behandling, behandlingSteg, null)
    }

    @Transactional
    fun håndterDistribuerVedtaksbrev(behandling: Behandling, journalpostId: String): Behandling {
        val behandlingSteg: DistribuerVedtaksbrevSteg = hentBehandlingSteg(DISTRIBUER_VEDTAKSBREV)

        return håndterSteg(behandling, behandlingSteg, journalpostId)
    }

    @Transactional
    fun håndterFerdigsitllBehandling(behandling: Behandling): Behandling {
        val behandlingSteg: FerdigstillBehandlingSteg = hentBehandlingSteg(FERDIGSTILLE_BEHANDLING)

        return håndterSteg(behandling, behandlingSteg, null)
    }


    @Transactional
    fun resetSteg(behandlingId: UUID, steg: StegType) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.status != BehandlingStatus.UTREDES) {
            error("Kan ikke endre steg når status=${behandling.status} behandling=$behandlingId")
        }
        if (!behandling.steg.kommerEtter(steg, behandling.type)) {
            error("Kan ikke sette behandling til steg=$steg når behandling allerede er på ${behandling.steg} behandling=$behandlingId")
        }

        validerAtStegKanResettes(behandling, steg)
        behandlingService.oppdaterStegPåBehandling(behandlingId, steg)
    }

    private fun validerAtStegKanResettes(behandling: Behandling,
                                         steg: StegType) {
        val harTilgangTilSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, behandling.steg.tillattFor)
        val harTilgangTilNesteSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, steg.tillattFor)
        if (!harTilgangTilSteg || !harTilgangTilNesteSteg) {
            val saksbehandler = SikkerhetContext.hentSaksbehandler()
            error("$saksbehandler kan ikke endre" +
                  " fra steg=${behandling.steg.displayName()} til steg=${steg.displayName()}" +
                  " pga manglende rolle på behandling=$behandling.id")
        }
    }

    // Generelle stegmetoder
    private fun <T> håndterSteg(behandling: Behandling,
                                behandlingSteg: BehandlingSteg<T>,
                                data: T): Behandling {
        val stegType = behandlingSteg.stegType()
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler()
        try {
            valider(behandling, stegType, saksbehandlerIdent, behandlingSteg)
            val nesteSteg = behandlingSteg.utførOgReturnerNesteSteg(behandling, data)
            oppdaterHistorikk(behandlingSteg, behandling, saksbehandlerIdent)
            oppdaterMetrikk(stegType, stegSuksessMetrics)
            validerNesteSteg(nesteSteg, behandling)
            logger.info("$stegType på behandling ${behandling.id} er håndtert")
            return behandlingService.oppdaterStegPåBehandling(behandlingId = behandling.id, steg = nesteSteg)
        } catch (exception: Exception) {
            oppdaterMetrikk(stegType, stegFeiletMetrics)
            logger.error("Håndtering av stegtype '$stegType' feilet på behandling ${behandling.id}.")
            throw exception
        }
    }

    private fun validerNesteSteg(nesteSteg: StegType, behandling: Behandling) {
        if (!nesteSteg.erGyldigIKombinasjonMedStatus(behandlingService.hentBehandling(behandling.id).status)) {
            error("Steg '${nesteSteg.displayName()}' kan ikke settes " +
                  "på behandling i kombinasjon med status ${behandling.status}")
        }
    }

    private fun <T> valider(behandling: Behandling,
                            stegType: StegType,
                            saksbehandlerIdent: String,
                            behandlingSteg: BehandlingSteg<T>) {
        validerHarTilgang(behandling, stegType, saksbehandlerIdent)

        validerGyldigTilstand(behandling, stegType, saksbehandlerIdent)

        utførBehandlingsvalidering(behandlingSteg, behandling)
    }

    private fun oppdaterMetrikk(stegType: StegType, metrikk: Map<StegType, Counter>) {
        metrikk[stegType]?.increment()
    }

    private fun <T> oppdaterHistorikk(behandlingSteg: BehandlingSteg<T>,
                                      behandling: Behandling,
                                      saksbehandlerIdent: String) {
        if (behandlingSteg.settInnHistorikk()) {
            behandlingshistorikkService.opprettHistorikkInnslag(
                    Behandlingshistorikk(behandlingId = behandling.id,
                                         steg = behandling.steg,
                                         opprettetAvNavn = SikkerhetContext.hentSaksbehandlerNavn(),
                                         opprettetAv = saksbehandlerIdent))
        }
    }

    private fun <T> utførBehandlingsvalidering(behandlingSteg: BehandlingSteg<T>,
                                               behandling: Behandling) {
        if (!behandlingSteg.stegType().erGyldigIKombinasjonMedStatus(behandling.status)) {
            error("Kan ikke utføre ${behandlingSteg.stegType()} når behandlingstatus er ${behandling.status}")
        }
        behandlingSteg.validerSteg(behandling)
    }

    private fun validerGyldigTilstand(behandling: Behandling,
                                      stegType: StegType,
                                      saksbehandlerIdent: String) {
        if (behandling.steg == BEHANDLING_FERDIGSTILT) {
            error("Behandlingen er avsluttet og stegprosessen kan ikke gjenåpnes")
        }

        if (stegType.kommerEtter(behandling.steg, behandling.type)) {
            error("$saksbehandlerIdent prøver å utføre steg '${stegType.displayName()}', " +
                  "men behandlingen er på steg '${behandling.steg.displayName()}'")
        }

        if (behandling.steg == BESLUTTE_VEDTAK && stegType != BESLUTTE_VEDTAK) {
            error("Behandlingen er på steg '${behandling.steg.displayName()}', og er da låst for alle andre type endringer.")
        }
    }

    private fun validerHarTilgang(behandling: Behandling,
                                  stegType: StegType,
                                  saksbehandlerIdent: String) {
        val harTilgangTilSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, behandling.steg.tillattFor)

        logger.info("Starter håndtering av $stegType på behandling ${behandling.id}")
        secureLogger.info("Starter håndtering av $stegType på behandling ${behandling.id} med saksbehandler=[$saksbehandlerIdent]")

        if (!harTilgangTilSteg) {
            error("$saksbehandlerIdent kan ikke utføre steg '${stegType.displayName()} pga manglende rolle.")
        }
    }

    private fun <T : BehandlingSteg<*>> hentBehandlingSteg(stegType: StegType): T {
        val firstOrNull = behandlingSteg.singleOrNull { it.stegType() == stegType }
                          ?: error("Finner ikke behandling steg for type $stegType")
        @Suppress("UNCHECKED_CAST")
        return firstOrNull as T
    }

    private fun initStegMetrikker(type: String): Map<StegType, Counter> {
        return behandlingSteg.map {
            it.stegType() to Metrics.counter("behandling.steg.$type",
                                             "steg",
                                             it.stegType().name,
                                             "beskrivelse",
                                             "${it.stegType().rekkefølge} ${it.stegType().displayName()}")
        }.toMap()
    }

}
