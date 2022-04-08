package no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
class BeregningBarnetilsynService {

    fun beregnYtelseBarnetilsyn(utgiftsperioder: List<UtgiftsperiodeDto>,
                                kontantstøttePerioder: List<PeriodeMedBeløpDto>,
                                tilleggsstønadsperioder: List<PeriodeMedBeløpDto>): List<BeløpsperiodeBarnetilsynDto> {

        validerGyldigePerioder(utgiftsperioder,
                               kontantstøttePerioder,
                               tilleggsstønadsperioder)

        return utgiftsperioder.map { it.split() }
                .flatten()
                .map { utgiftsMåned ->
                    utgiftsMåned.tilBeløpsperiodeBarnetilsynDto(kontantstøttePerioder,
                                                                tilleggsstønadsperioder)
                }
                .mergeSammenhengendePerioder()
    }

    fun List<PeriodeMedBeløpDto>.tilPerioder(): List<Periode> =
            this.map {
                it.tilPeriode()
            }

    private fun validerGyldigePerioder(utgiftsperioderDto: List<UtgiftsperiodeDto>,
                                       kontantstøttePerioderDto: List<PeriodeMedBeløpDto>,
                                       tilleggsstønadsperioderDto: List<PeriodeMedBeløpDto>) {

        val utgiftsperioder = utgiftsperioderDto.tilPerioder()
        val kontantstøttePerioder = kontantstøttePerioderDto.tilPerioder()
        val tilleggsstønadsperioder = tilleggsstønadsperioderDto.tilPerioder()

        brukerfeilHvis(utgiftsperioder.isEmpty()) { "Ingen utgiftsperioder" }
        brukerfeilHvis(harUrelevantReduksjonsPeriode(utgiftsperioder,
                                                     kontantstøttePerioder)) { "Urelevant kontantstøtteperiode kan fjernes" }
        brukerfeilHvis(harUrelevantReduksjonsPeriode(utgiftsperioder,
                                                     tilleggsstønadsperioder)) { "Urelevant tilleggsstønadsperiode kan fjernes" }
        brukerfeilHvis(utgiftsperioder.harOverlappende()) { "Utgiftsperioder $utgiftsperioder overlapper" }
        brukerfeilHvis((kontantstøttePerioder.harOverlappende())) { "Kontantstøtteperioder $kontantstøttePerioder overlapper" }
        brukerfeilHvis((tilleggsstønadsperioder.harOverlappende())) { "Tilleggsstønadsperioder $tilleggsstønadsperioder overlapper" }
    }

    private fun harUrelevantReduksjonsPeriode(utgiftsperioder: List<Periode>, reduksjonsperioder: List<Periode>): Boolean {
        return reduksjonsperioder.isNotEmpty() && !reduksjonsperioder.any {
            utgiftsperioder.any { ut ->
                ut.overlapper(it)
            }
        }
    }
}

private fun List<Periode>.harOverlappende(): Boolean {
    val sortedBy = this.sortedBy { it.fradato }
    return sortedBy.zipWithNext { a, b ->
        a.overlapper(b)
    }.any { it }
}

/**
 * Del opp utgiftsperioder i atomiske deler (mnd).
 * Eksempel: 1stk UtgiftsperiodeDto fra januar til mars deles opp i 3:
 * listOf(UtgiftsMåned(jan), UtgiftsMåned(feb), UtgiftsMåned(mars))
 */
fun UtgiftsperiodeDto.split(): List<UtgiftsMåned> {
    val perioder = mutableListOf<UtgiftsMåned>()
    var måned = this.årMånedFra
    while (måned <= this.årMånedTil) {
        perioder.add(UtgiftsMåned(måned, this.barn, this.utgifter))
        måned = måned.plusMonths(1)
    }
    return perioder
}

/**
 * Merger sammenhengende perioder hvor beløp og @BeløpsperiodeBarnetilsynDto#beregningsgrunnlag (it.toKey()) er like.
 */
fun List<BeløpsperiodeBarnetilsynDto>.mergeSammenhengendePerioder(): List<BeløpsperiodeBarnetilsynDto> {
    val sortertPåDatoListe = this.sortedBy { it.periode.fradato }
    return sortertPåDatoListe.fold(mutableListOf()) { acc, entry ->
        val last = acc.lastOrNull()
        if (last != null && last.hengerSammenMed(entry) && last.sammeBeløpOgBeregningsgrunnlag(entry)) {
            acc.removeLast()
            acc.add(last.copy(periode = last.periode.copy(tildato = entry.periode.tildato)))
        } else {
            acc.add(entry)
        }
        acc
    }
}

fun BeløpsperiodeBarnetilsynDto.hengerSammenMed(other: BeløpsperiodeBarnetilsynDto): Boolean {
    val firstDatePlussEnMnd = this.periode.tildato.plusMonths(1)
    return firstDatePlussEnMnd.yearMonth() == other.periode.fradato.yearMonth()
}

fun BeløpsperiodeBarnetilsynDto.sammeBeløpOgBeregningsgrunnlag(other: BeløpsperiodeBarnetilsynDto) =
        this.beløp == other.beløp &&
        this.beregningsgrunnlag == other.beregningsgrunnlag

private fun UtgiftsMåned.tilBeløpsperiodeBarnetilsynDto(kontantstøttePerioder: List<PeriodeMedBeløpDto>,
                                                        tilleggsstønadsperioder: List<PeriodeMedBeløpDto>): BeløpsperiodeBarnetilsynDto {
    val kontantStøtteBeløp = kontantstøttePerioder.finnPeriodeBeløp(this)
    val tilleggsstønadsperiodeBeløp = tilleggsstønadsperioder.finnPeriodeBeløp(this)

    return BeregningBarnetilsynUtil.lagBeløpsPeriodeBarnetilsyn(utgiftsperiode = this,
                                                                kontantstøtteBeløp = BigDecimal(kontantStøtteBeløp),
                                                                tilleggsstønadBeløp = BigDecimal(tilleggsstønadsperiodeBeløp),
                                                                antallBarnIPeriode = this.barn.size)
}

private fun List<PeriodeMedBeløpDto>.finnPeriodeBeløp(utgiftsMåned: UtgiftsMåned): Int {
    return this.find { utgiftsMåned.omsluttesAv(it) }?.beløp ?: 0
}

private fun UtgiftsMåned.omsluttesAv(it: PeriodeMedBeløpDto) = this.årMåned.omsluttesAv(it.årMånedFra, it.årMånedTil)

fun YearMonth.omsluttesAv(fraOgMed: YearMonth, tilOgMed: YearMonth): Boolean = fraOgMed <= this && this <= tilOgMed

private fun LocalDate.yearMonth(): YearMonth = YearMonth.from(this)



