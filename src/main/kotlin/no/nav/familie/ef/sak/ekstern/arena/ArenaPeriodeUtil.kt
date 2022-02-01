package no.nav.familie.ef.sak.ekstern.arena

import no.nav.familie.ef.sak.infotrygd.InternePerioder
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import java.time.LocalDate
import java.time.YearMonth

/**
 * Denne erstatter tjenesten som Arena kaller idag i Infotrygd.
 * https://confluence.adeo.no/pages/viewpage.action?pageId=395741283#
 * Denne tjenesten henter perioder fra de ulike stønadene og slår sammen de,
 * tvers stønadstyper og returnerer perioder som overlapper datoer i input
 *
 * Den tjenesten filtrerte tidligere ut perioder som har stønadsbeløp > 0,
 * dette har vi blitt enige om å ikke gjøre i denne, nye tjenesten
 */
object ArenaPeriodeUtil {

    fun slåSammenPerioderFraEfOgInfotrygd(request: PerioderOvergangsstønadRequest,
                                          perioder: InternePerioder): List<PeriodeOvergangsstønad> {
        val måneder = finnUnikeÅrMånedForPerioder(perioder)
        val sammenslåtteÅrMåneder = slåSammenÅrMåneder(måneder)
        return sammenslåtteÅrMåneder.map {
            PeriodeOvergangsstønad(personIdent = request.personIdent,
                                   fomDato = it.first.atDay(1),
                                   tomDato = it.second.atEndOfMonth(),
                                   datakilde = PeriodeOvergangsstønad.Datakilde.EF)
        }.filter { overlapper(request, it) }
    }

    private fun overlapper(request: PerioderOvergangsstønadRequest, periode: PeriodeOvergangsstønad): Boolean {
        val requestFom = request.fomDato ?: LocalDate.now() // Arena sender alltid fom/tom-datoer, burde endre kontraktet
        val requestTom = request.tomDato ?: LocalDate.now()
        val range = periode.fomDato..periode.tomDato
        return requestFom in range
               || requestTom in range
               || (requestFom < periode.fomDato && requestTom > periode.tomDato) // omslutter
    }

    private fun slåSammenÅrMåneder(måneder: MutableSet<YearMonth>): MutableList<Pair<YearMonth, YearMonth>> {
        return måneder.toList().sorted().fold(mutableListOf()) { acc, yearMonth ->
            val last = acc.lastOrNull()
            if (last == null || last.second.plusMonths(1) != yearMonth) {
                acc.add(Pair(yearMonth, yearMonth))
            } else {
                acc.removeLast()
                acc.add(last.copy(second = yearMonth))
            }
            acc
        }
    }

    private fun finnUnikeÅrMånedForPerioder(perioder: InternePerioder): MutableSet<YearMonth> {
        val måneder = mutableSetOf<YearMonth>()
        (perioder.overgangsstønad + perioder.barnetilsyn + perioder.skolepenger).forEach {
            var start: LocalDate = it.stønadFom
            while (start <= it.stønadTom) {
                måneder.add(YearMonth.from(start))
                start = start.plusMonths(1)
            }
        }
        return måneder
    }

}