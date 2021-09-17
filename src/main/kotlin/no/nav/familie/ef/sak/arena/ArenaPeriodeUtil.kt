package no.nav.familie.ef.sak.arena

import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.felles.util.isEqualOrBefore
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeOvergangsstønad
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad.Datakilde
import java.time.LocalDate
import java.time.YearMonth
import java.util.Stack

object ArenaPeriodeUtil {

    /**
     * Skal filtrere bort de som har beløp = 0
     * Skal filtere bort de som har tomdato < fomDato || opphørdato < tomDato
     */
    fun mapOgFiltrer(infotrygdPerioder: InfotrygdPerioderOvergangsstønadResponse) =
            infotrygdPerioder.perioder.filter { it.beløp > 0 }.map {
                PeriodeOvergangsstønad(personIdent = it.personIdent,
                                       fomDato = it.fomDato,
                                       tomDato = it.opphørsdatoEllerTomDato(),
                                       datakilde = Datakilde.INFOTRYGD)
            }.filterNot { it.tomDato.isBefore(it.fomDato) }

    fun mapOgFiltrer(andeler: List<AndelTilkjentYtelse>) =
            andeler.filter { it.beløp > 0 }.map {
                PeriodeOvergangsstønad(personIdent = it.personIdent,
                                       fomDato = it.stønadFom,
                                       tomDato = it.stønadTom,
                                       datakilde = Datakilde.EF)
            }

    private fun InfotrygdPeriodeOvergangsstønad.opphørsdatoEllerTomDato(): LocalDate {
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
    fun slåSammenPerioder(perioder: List<PeriodeOvergangsstønad>): List<PeriodeOvergangsstønad> {
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
}