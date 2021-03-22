package no.nav.familie.ef.sak.service

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.familie.ef.sak.exception.PdlNotFoundException
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.util.isEqualOrAfter
import no.nav.familie.ef.sak.util.isEqualOrBefore
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeOvergangsstønad
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.util.Stack

/**
 * Denne tjenesten henter perioder for andre typer stønader fra EF også
 * Arena kaller idag en endepunkt hos infotrygd som returnerer perioder fra alle stønader, men det var sagt i starten att de
 * skulle hente perioder for overgangsstønad fra oss.
 * Lar den hete PerioderOvergangsstønad tils videre då det ikke er helt avklart hva de egentlige skal ha
 */
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
            sjekkOmDetFinnesDiffIPerioder(request.personIdent, response, responseFraReplika)
            response
        }
    }

    fun hentReplikaPerioder(request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        val personIdenter = hentPersonIdenter(request)
        return hentPerioderFraReplika(personIdenter, request)
    }

    private fun sjekkOmDetFinnesDiffIPerioder(fnr: String,
                                              response: PerioderOvergangsstønadResponse,
                                              responseFraReplika: PerioderOvergangsstønadResponse) {
        val replikaPerioder = responseFraReplika.perioder.sortedBy { it.fomDato }.map { it.fomDato to it.tomDato }
        val perioder = response.perioder.sortedBy { it.fomDato }.map { it.fomDato to it.tomDato }
        if (perioder != replikaPerioder) {
            logger.warn("Det finnes forskjell i periodene fra infotrygd og replika")
            secureLogger.info("Diff i perioder for fnr=$fnr perioder=$perioder replikaPerioder=$replikaPerioder")
        }
    }

    private fun hentPerioderFraReplika(personIdenter: Set<String>,
                                       request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        val infotrygdRequest = InfotrygdPerioderOvergangsstønadRequest(personIdenter, request.fomDato, request.tomDato)
        val infotrygdPerioder = infotrygdReplikaClient.hentPerioderOvergangsstønad(infotrygdRequest)
        val perioder = mapOgFiltrer(infotrygdPerioder)
        return PerioderOvergangsstønadResponse(slåSammenPerioder(perioder))
    }

    /**
     * Skal filtrere bort de som har beløp = 0
     * Skal filtere bort de som har tomdato < fomDato || opphørdato < tomDato
     */
    private fun mapOgFiltrer(infotrygdPerioder: InfotrygdPerioderOvergangsstønadResponse) =
            infotrygdPerioder.perioder.filter { it.beløp > 0 }.map {
                PeriodeOvergangsstønad(personIdent = it.personIdent,
                                       fomDato = it.fomDato,
                                       tomDato = it.opphørsdatoEllerTomDato(),
                                       datakilde = PeriodeOvergangsstønad.Datakilde.INFOTRYGD)
            }.filterNot { it.tomDato.isBefore(it.fomDato) }

    fun InfotrygdPeriodeOvergangsstønad.opphørsdatoEllerTomDato(): LocalDate {
        val opphørsdato = this.opphørsdato
        return if (opphørsdato != null && opphørsdato.isBefore(tomDato)) {
            opphørsdato
        } else {
            tomDato
        }
    }

    /**
     * Slår sammen perioder som er sammenhengende og overlappende.
     * Dette er noe som idag gjøres i infotrygd men er ikke sikkert burde gjøres når vi henter perioder fra vår egen database
     */
    private fun slåSammenPerioder(perioder: List<PeriodeOvergangsstønad>): List<PeriodeOvergangsstønad> {
        val mergedePerioder = Stack<PeriodeOvergangsstønad>()
        perioder.sortedBy { it.fomDato }.forEach { period ->
            if (mergedePerioder.isEmpty()) {
                mergedePerioder.push(period)
            }
            val last = mergedePerioder.peek()
            if (erSammenhengendeEllerOverlappende(last, period)) {
                mergedePerioder.push(mergedePerioder.pop().copy(fomDato = minOf(last.fomDato, period.fomDato),
                                                                tomDato = maxOf(last.tomDato, period.tomDato)))
            } else {
                mergedePerioder.push(period)
            }
        }
        return mergedePerioder
    }

    private fun erSammenhengendeEllerOverlappende(last: PeriodeOvergangsstønad,
                                                  period: PeriodeOvergangsstønad) =
            sammenhengendePeriode(last, period) || erOverlappende(last, period)

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