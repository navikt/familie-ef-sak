package no.nav.familie.ef.sak.service.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ef.sak.api.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.config.RolleConfig
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.BehandlingshistorikkService
import no.nav.familie.ef.sak.service.GrunnlagsdataService
import no.nav.familie.ef.sak.service.steg.StegType.*
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.task.FerdigstillBehandlingTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

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
    fun håndterRegistrerOpplysninger(behandling: Behandling, søknad: String?): Behandling {
        val behandlingSteg: RegistrereOpplysningerSteg = hentBehandlingSteg(REGISTRERE_OPPLYSNINGER)
        return håndterSteg(behandling, behandlingSteg, søknad)
    }

    @Transactional
    fun håndterInngangsvilkår(behandling: Behandling): Behandling {
        val behandlingSteg: InngangsvilkårSteg = hentBehandlingSteg(VILKÅRSVURDERE_INNGANGSVILKÅR)
        return håndterSteg(behandling, behandlingSteg, null)
    }

    @Transactional
    fun håndterStønadsvilkår(behandling: Behandling): Behandling {
        val behandlingSteg: StønadsvilkårSteg = hentBehandlingSteg(VILKÅRSVURDERE_STØNAD)
        return håndterSteg(behandling, behandlingSteg, null)
    }

    @Transactional
    fun håndterBeregnYtelseForStønad(behandling: Behandling, tilkjentYtelse: TilkjentYtelseDTO): Behandling {
        val behandlingSteg: BeregnYtelseSteg = hentBehandlingSteg(BEREGNE_YTELSE)
        return håndterSteg(behandling, behandlingSteg, tilkjentYtelse)
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
    fun håndterDistribuerVedtaksbrev(behandling: Behandling): Behandling {
        val behandlingSteg: JournalførVedtaksbrevSteg = hentBehandlingSteg(DISTRIBUER_VEDTAKSBREV)

        return håndterSteg(behandling, behandlingSteg, null)
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
            error("Kan ikke endre steg når status=${behandling.status}")
        }
        if (!behandling.steg.kommerEtter(steg, behandling.type)) {
            error("Kan ikke sette behandling til steg=$steg når behandling allerede er på ${behandling.steg}")
        }

        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val harTilgangTilSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, behandling.steg.tillattFor)
        val harTilgangTilNesteSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, steg.tillattFor)
        if (!harTilgangTilSteg || !harTilgangTilNesteSteg) {
            error("$saksbehandler kan ikke endre fra steg=${behandling.steg.displayName()} til steg=${steg.displayName()} pga manglende rolle.")
        }
        behandlingService.oppdaterStegPåBehandling(behandlingId, steg)
    }

    // Generelle stegmetoder
    private fun <T> håndterSteg(behandling: Behandling,
                                behandlingSteg: BehandlingSteg<T>,
                                data: T): Behandling {
        val stegType = behandlingSteg.stegType()
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler()
        try {
            val harTilgangTilSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, behandling.steg.tillattFor)

            logger.info("Starter håndtering av $stegType på behandling ${behandling.id}")
            secureLogger.info("Starter håndtering av $stegType på behandling ${behandling.id} med saksbehandler=[$saksbehandlerIdent]")

            if (!harTilgangTilSteg) {
                error("$saksbehandlerIdent kan ikke utføre steg '${stegType.displayName()} pga manglende rolle.")
            }

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

            behandlingSteg.validerSteg(behandling)

            val nesteSteg = behandlingSteg.utførOgReturnerNesteSteg(behandling, data)

            if (behandlingSteg.settInnHistorikk()) {
                behandlingshistorikkService.opprettHistorikkInnslag(
                        Behandlingshistorikk(behandlingId = behandling.id,
                                             steg = behandling.steg,
                                             opprettetAvNavn = SikkerhetContext.hentSaksbehandlerNavn(),
                                             opprettetAv = saksbehandlerIdent))
            }

            stegSuksessMetrics[stegType]?.increment()

            if (!nesteSteg.erGyldigIKombinasjonMedStatus(behandlingService.hentBehandling(behandling.id).status)) {
                error("Steg '${nesteSteg.displayName()}' kan ikke settes " +
                      "på behandling i kombinasjon med status ${behandling.status}")
            }

            val returBehandling = behandlingService.oppdaterStegPåBehandling(behandlingId = behandling.id, steg = nesteSteg)

            logger.info("$stegType på behandling ${behandling.id} er håndtert")
            return returBehandling
        } catch (exception: Exception) {
            stegFeiletMetrics[stegType]?.increment()
            logger.error("Håndtering av stegtype '$stegType' feilet på behandling ${behandling.id}.")
            throw exception
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
