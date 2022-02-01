package no.nav.familie.ef.sak.ekstern.arena

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.familie.ef.sak.infotrygd.PeriodeService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Denne tjenesten henter perioder for andre typer stønader fra EF også
 * Arena kaller idag en endepunkt hos infotrygd som returnerer perioder fra alle stønader, men det var sagt i starten att de
 * skulle hente perioder for overgangsstønad fra oss.
 */
@Service
class ArenaStønadsperioderService(
        private val periodeService: PeriodeService,
        private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    /**
     * Henter perioder fra infotrygd for en person
     * Skal hente perioder fra ef-sak også i fremtiden
     */
    fun hentPerioder(request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        return runBlocking {
            val asyncResponse = async { personopplysningerIntegrasjonerClient.hentInfotrygdPerioder(request) }
            val perioderFraITogEF = async { slåSammenPerioderFraEfOgInfotrygd(request) }
            val perioderFraInfotrygd = asyncResponse.await().perioder
            val perioder = perioderFraITogEF.await()
            sjekkDiff(request, perioder, perioderFraInfotrygd)
            // TODO erstatt med perioderFraITogEF
            PerioderOvergangsstønadResponse(perioderFraInfotrygd)
        }
    }

    private fun slåSammenPerioderFraEfOgInfotrygd(request: PerioderOvergangsstønadRequest): List<PeriodeOvergangsstønad> {
        val perioder = periodeService.hentPerioderFraEfOgInfotrygd(request.personIdent)
        return ArenaPeriodeUtil.slåSammenPerioderFraEfOgInfotrygd(request, perioder)
    }

    private fun sjekkDiff(request: PerioderOvergangsstønadRequest,
                          perioderFraReplika: List<PeriodeOvergangsstønad>,
                          perioderFraInfotrygd: List<PeriodeOvergangsstønad>) {
        if (perioderFraReplika != perioderFraInfotrygd) {
            logger.warn("Diff i perioder mellom infotrygd og replika")
            secureLogger.warn("Diff i perioder mellom infotrygd og replika for request={} - infotrygd={} replika={}",
                              request, perioderFraInfotrygd, perioderFraReplika)
        }
    }

}