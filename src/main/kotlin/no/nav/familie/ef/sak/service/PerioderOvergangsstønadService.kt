package no.nav.familie.ef.sak.service

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.familie.ef.sak.exception.PdlNotFoundException
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.util.isEqualOrAfter
import no.nav.familie.ef.sak.util.isEqualOrBefore
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class PerioderOvergangsstønadService(private val infotrygdReplikaClient: InfotrygdReplikaClient,
                                     private val familieIntegrasjonerClient: FamilieIntegrasjonerClient,
                                     private val pdlClient: PdlClient) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    /**
     * Henter perioder fra infotrygd for en person
     * Skal hente perioder fra ef-sak også i fremtiden
     */
    fun hentPerioder(request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        return runBlocking {
            val asyncResponse = async { familieIntegrasjonerClient.hentInfotrygdPerioder(request) }
            val responseFraReplika = hentReplikaPerioder(request)
            val response = asyncResponse.await()
            diffResponse(request.personIdent, response, responseFraReplika)
            response
        }
    }

    fun hentReplikaPerioder(request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        val personIdenter = hentPersonIdenter(request)
        return hentPerioderFraReplika(personIdenter, request)
    }

    private fun diffResponse(fnr: String,
                             response: PerioderOvergangsstønadResponse,
                             responseFraReplika: PerioderOvergangsstønadResponse) {
        val replikaPerioder = responseFraReplika.perioder.sortedBy { it.fomDato }.map { it.fomDato to it.tomDato }
        val perioder = response.perioder.sortedBy { it.fomDato }.map { it.fomDato to it.tomDato }
        if (perioder != replikaPerioder) {
            logger.warn("Diff in periods")
            secureLogger.info("Diff for fnr=$fnr perioder=$perioder replikaPerioder=$replikaPerioder")
        }
    }

    private fun hentPerioderFraReplika(personIdenter: Set<String>,
                                       request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        val infotrygdRequest = InfotrygdPerioderOvergangsstønadRequest(personIdenter, request.fomDato, request.tomDato)
        val infotrygdPerioder = infotrygdReplikaClient.hentPerioderOvergangsstønad(infotrygdRequest)
        val perioder = infotrygdPerioder.perioder.filter { it.beløp > 0 }.map {
            val tomDato = it.opphørsdato?.let { opphørsdato -> if (opphørsdato.isBefore(it.tomDato)) opphørsdato else it.tomDato }
                          ?: it.tomDato
            PeriodeOvergangsstønad(personIdent = it.personIdent,
                                   fomDato = it.fomDato,
                                   tomDato = tomDato,
                                   datakilde = PeriodeOvergangsstønad.Datakilde.INFOTRYGD)
        }.filterNot { it.tomDato.isBefore(it.fomDato) }.sortedBy { it.fomDato }
        return PerioderOvergangsstønadResponse(slåSammenPerioder(perioder))
    }

    /**
     * Slår sammen perioder som er sammenhengende og overlappende.
     */
    private fun slåSammenPerioder(perioder: List<PeriodeOvergangsstønad>): List<PeriodeOvergangsstønad> {
        val mergedePerioder = mutableListOf<PeriodeOvergangsstønad>()
        perioder.forEach { period ->
            if (mergedePerioder.isEmpty()) {
                mergedePerioder.add(period)
                return@forEach
            }
            val last = mergedePerioder.last()
            if (sammenhengendePeriode(last, period)) {
                mergedePerioder[mergedePerioder.size - 1] = last.copy(tomDato = maxOf(last.tomDato, period.tomDato))
            } else if (erOverlappende(last, period)) {
                mergedePerioder[mergedePerioder.size - 1] = last.copy(fomDato = minOf(last.fomDato, period.fomDato),
                                                                      tomDato = maxOf(last.tomDato, period.tomDato))
            } else {
                mergedePerioder.add(period)
            }
        }
        return mergedePerioder
    }

    /**
     * En periode er sammenhengende hvis perioden er i den samme måneden, eller om måned + 1 er lik
     */
    private fun sammenhengendePeriode(first: PeriodeOvergangsstønad, second: PeriodeOvergangsstønad): Boolean {
        val firstTomDato = YearMonth.from(first.tomDato)
        val secondFom = YearMonth.from(second.fomDato)
        return firstTomDato == secondFom || firstTomDato.plusMonths(1) == secondFom
    }

    private fun erOverlappende(mergedPeriode: PeriodeOvergangsstønad, period: PeriodeOvergangsstønad) =
            mergedPeriode.fomDato.isEqualOrBefore(period.tomDato) && mergedPeriode.tomDato.isEqualOrAfter(period.fomDato)

    private fun hentPersonIdenter(request: PerioderOvergangsstønadRequest): Set<String> {
        return try {
            pdlClient.hentPersonidenter(request.personIdent, true).identer.map { it.ident }.toSet()
        } catch (e: PdlNotFoundException) {
            logger.warn("Finner ikke person, returnerer personIdent i request")
            setOf(request.personIdent)
        }
    }

}