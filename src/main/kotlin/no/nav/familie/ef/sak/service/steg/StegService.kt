package no.nav.familie.ef.sak.service.steg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ef.sak.config.RolleConfig
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.service.BehandlingHistorikkService
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.steg.StegType.BEHANDLING_FERDIGSTILT
import no.nav.familie.ef.sak.service.steg.StegType.VILKÅRSVURDERE_INNGANGSVILKÅR
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StegService(private val behandlingSteg: List<BehandlingSteg<*>>,
                  private val behandlingService: BehandlingService,
                  private val rolleConfig: RolleConfig,
                  private val behandlingHistorikkService: BehandlingHistorikkService) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")


    private val stegSuksessMetrics: Map<StegType, Counter> = initStegMetrikker("suksess")

    private val stegFeiletMetrics: Map<StegType, Counter> = initStegMetrikker("feil")

    @Transactional
    fun håndterRegistrerOpplysninger(behandling: Behandling, søknad: String): Behandling {
        val behandlingSteg: RegistrereOpplysningerSteg = hentBehandlingSteg(StegType.REGISTRERE_OPPLYSNINGER)
        return håndterSteg(behandling, behandlingSteg, søknad)
    }

    @Transactional
    fun håndterInngangsvilkår(behandling: Behandling): Behandling {
        val behandlingSteg: InngangsvilkårSteg = hentBehandlingSteg(VILKÅRSVURDERE_INNGANGSVILKÅR)
        return håndterSteg(behandling, behandlingSteg, null)
    }

    // Generelle stegmetoder
    private fun <T> håndterSteg(behandling: Behandling,
                                behandlingSteg: BehandlingSteg<T>,
                                data: T): Behandling {
        val stegType = behandlingSteg.stegType()
        try {
            val saksbehandlerNavn = SikkerhetContext.hentSaksbehandlerNavn()
            val harTilgangTilSteg = SikkerhetContext.harTilgangTilGittRolle(rolleConfig, behandling.steg.tillattFor)

            logger.info("$saksbehandlerNavn håndterer $stegType på behandling ${behandling.id}")
            if (!harTilgangTilSteg) {
                error("$saksbehandlerNavn kan ikke utføre steg '${stegType.displayName()} pga manglende rolle.")
            }

            if (behandling.steg == BEHANDLING_FERDIGSTILT) {
                error("Behandlingen er avsluttet og stegprosessen kan ikke gjenåpnes")
            }

            if (stegType.kommerEtter(behandling.steg, behandling.type)) {
                error("$saksbehandlerNavn prøver å utføre steg '${stegType.displayName()}', " +
                      "men behandlingen er på steg '${behandling.steg.displayName()}'")
            }

            if (behandling.steg == StegType.BESLUTTE_VEDTAK && stegType != StegType.BESLUTTE_VEDTAK) {
                error("Behandlingen er på steg '${behandling.steg.displayName()}', og er da låst for alle andre type endringer.")
            }

            behandlingSteg.validerSteg(behandling)

            behandlingHistorikkService.opprettHistorikkInnslag(Behandlingshistorikk(behandlingId = behandling.id,
                                                                                    steg = behandling.steg,
                                                                                    endretAvNavn = saksbehandlerNavn,
                                                                                    endretAvMail = SikkerhetContext.hentSaksbehandler()))

            val nesteSteg = behandlingSteg.utførOgReturnerNesteSteg(behandling, data)

            stegSuksessMetrics[stegType]?.increment()

            if (nesteSteg == BEHANDLING_FERDIGSTILT) {
                logger.info("$saksbehandlerNavn er ferdig med stegprosess på behandling ${behandling.id}")
            }

            if (!nesteSteg.erGyldigIKombinasjonMedStatus(behandlingService.hentBehandling(behandling.id).status)) {
                error("Steg '${nesteSteg.displayName()}' kan ikke settes " +
                      "på behandling i kombinasjon med status ${behandling.status}")
            }

            val returBehandling = behandlingService.oppdaterStegPåBehandling(behandlingId = behandling.id, steg = nesteSteg)

            logger.info("$saksbehandlerNavn har håndtert $stegType på behandling ${behandling.id}")
            return returBehandling
        } catch (exception: Exception) {
            stegFeiletMetrics[stegType]?.increment()
            logger.error("Håndtering av stegtype '$stegType' feilet på behandling ${behandling.id}.")
            secureLogger.info("Håndtering av stegtype '$stegType' feilet.", exception)
            throw exception
        }
    }

    fun <T : BehandlingSteg<*>> hentBehandlingSteg(stegType: StegType): T {
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
