package no.nav.familie.ef.sak.beregning.barnetilsyn

import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.harOverlappende
import org.springframework.stereotype.Service
import java.time.Month
import java.time.YearMonth

@Service
class BeregningBarnetilsynService(
    private val featureToggleService: FeatureToggleService,
) {
    fun beregnYtelseBarnetilsyn(
        utgiftsperioder: List<UtgiftsperiodeDto>,
        kontantstøttePerioder: List<PeriodeMedBeløpDto>,
        tilleggsstønadsperioder: List<PeriodeMedBeløpDto>,
        erMigrering: Boolean = false,
    ): List<BeløpsperiodeBarnetilsynDto> {
        validerGyldigePerioder(utgiftsperioder, kontantstøttePerioder, tilleggsstønadsperioder, erMigrering)
        validerFornuftigeBeløp(utgiftsperioder, kontantstøttePerioder, tilleggsstønadsperioder)

        val brukIkkeVedtatteSatser = featureToggleService.isEnabled(Toggle.SATSENDRING_BRUK_IKKE_VEDTATT_MAXSATS)
        return utgiftsperioder
            .tilBeløpsperioderPerUtgiftsmåned(
                kontantstøttePerioder,
                tilleggsstønadsperioder,
                brukIkkeVedtatteSatser,
            ).values
            .toList()
            .mergeSammenhengendePerioder()
    }

    /**
     * Hva som er "fornuftige" beløp kan sikkert endre seg. Mulig vi kan justere beløp for å treffe bedre etterhvert.
     * Denne valideringen vil bare fange opp relativt store overskidelser av hva vi anser som fornuftig.
     */
    private fun validerFornuftigeBeløp(
        utgiftsperioder: List<UtgiftsperiodeDto>,
        kontantstøttePerioder: List<PeriodeMedBeløpDto>,
        tilleggsstønadsperioder: List<PeriodeMedBeløpDto>,
    ) {
        brukerfeilHvis(utgiftsperioder.any { it.utgifter < 0 }) { "Utgifter kan ikke være mindre enn 0" }
        brukerfeilHvis(utgiftsperioder.any { it.utgifter > 40000 }) { "Utgifter på mer enn 40000 støttes ikke" }

        brukerfeilHvis(kontantstøttePerioder.any { it.beløp < 0 }) { "Kontantstøtte kan ikke være mindre enn 0" }
        brukerfeilHvis(kontantstøttePerioder.any { it.beløp > 45000 }) { "Kontantstøtte på over 45000 pr. mnd støttes ikke" }

        brukerfeilHvis(tilleggsstønadsperioder.any { it.beløp < 0 }) { "Beløp tilleggsstønad kan ikke være mindre enn 0" }
        brukerfeilHvis(tilleggsstønadsperioder.any { it.beløp > 20000 }) { "Tilleggsstønad større enn 20000 støttes ikke" }
    }

    fun List<PeriodeMedBeløpDto>.tilPerioder(): List<Månedsperiode> =
        this.map {
            it.periode
        }

    private fun validerGyldigePerioder(
        utgiftsperioderDto: List<UtgiftsperiodeDto>,
        kontantstøttePerioderDto: List<PeriodeMedBeløpDto>,
        tilleggsstønadsperioderDto: List<PeriodeMedBeløpDto>,
        erMigrering: Boolean,
    ) {
        val utgiftsperioder = utgiftsperioderDto.tilPerioder()
        val kontantstøttePerioder = kontantstøttePerioderDto.tilPerioder()
        val tilleggsstønadsperioder = tilleggsstønadsperioderDto.tilPerioder()

        brukerfeilHvis(utgiftsperioder.isEmpty()) {
            "Ingen utgiftsperioder"
        }

        brukerfeilHvis(harUrelevantReduksjonsPeriode(utgiftsperioder, kontantstøttePerioder)) {
            "Urelevant kontantstøtteperiode kan fjernes"
        }

        brukerfeilHvis(harUrelevantReduksjonsPeriode(utgiftsperioder, tilleggsstønadsperioder)) {
            "Urelevant tilleggsstønadsperiode kan fjernes"
        }

        brukerfeilHvis(utgiftsperioder.harOverlappende()) {
            "Utgiftsperioder $utgiftsperioder overlapper"
        }

        brukerfeilHvis((kontantstøttePerioder.harOverlappende())) {
            "Kontantstøtteperioder $kontantstøttePerioder overlapper"
        }

        brukerfeilHvis((tilleggsstønadsperioder.harOverlappende())) {
            "Tilleggsstønadsperioder $tilleggsstønadsperioder overlapper"
        }

        val innføringsMndKontantstøttefradrag = YearMonth.of(2020, Month.MARCH)
        brukerfeilHvis((kontantstøttePerioder.harPeriodeFør(innføringsMndKontantstøttefradrag))) {
            "Fradrag for innvilget kontantstøtte trår i kraft: $innføringsMndKontantstøttefradrag"
        }

        val manglerAktivitetstype =
            utgiftsperioderDto.any { it.periodetype == PeriodetypeBarnetilsyn.ORDINÆR && it.aktivitetstype == null }
        brukerfeilHvis(!erMigrering && manglerAktivitetstype) {
            "Utgiftsperioder $utgiftsperioderDto mangler en eller flere aktivitetstyper"
        }
    }

    private fun harUrelevantReduksjonsPeriode(
        utgiftsperioder: List<Månedsperiode>,
        reduksjonsperioder: List<Månedsperiode>,
    ): Boolean =
        reduksjonsperioder.isNotEmpty() &&
            !reduksjonsperioder.any {
                utgiftsperioder.any { ut ->
                    ut.overlapper(it)
                }
            }
}

private fun List<Månedsperiode>.harPeriodeFør(årMåned: YearMonth): Boolean = this.any { it.fom < årMåned }

fun InnvilgelseBarnetilsyn.tilBeløpsperioderPerUtgiftsmåned(brukIkkeVedtatteSatser: Boolean) =
    this.perioder.tilBeløpsperioderPerUtgiftsmåned(
        this.perioderKontantstøtte,
        this.tilleggsstønad.perioder,
        brukIkkeVedtatteSatser,
    )

fun List<UtgiftsperiodeDto>.tilBeløpsperioderPerUtgiftsmåned(
    kontantstøttePerioder: List<PeriodeMedBeløpDto>,
    tilleggsstønadsperioder: List<PeriodeMedBeløpDto>,
    brukIkkeVedtatteSatser: Boolean,
) = this
    .map { it.split() }
    .flatten()
    .associate { utgiftsMåned ->
        utgiftsMåned.årMåned to
            utgiftsMåned.tilBeløpsperiodeBarnetilsynDto(
                kontantstøttePerioder,
                tilleggsstønadsperioder,
                brukIkkeVedtatteSatser,
            )
    }

/**
 * Del opp utgiftsperioder i atomiske deler (mnd).
 * Eksempel: 1stk UtgiftsperiodeDto fra januar til mars deles opp i 3:
 * listOf(UtgiftsMåned(jan), UtgiftsMåned(feb), UtgiftsMåned(mars))
 */
fun UtgiftsperiodeDto.split(): List<UtgiftsMåned> {
    val perioder = mutableListOf<UtgiftsMåned>()
    var måned = this.periode.fom
    while (måned <= this.periode.tom) {
        val utgifter = this.utgifter.toBigDecimal()
        perioder.add(UtgiftsMåned(måned, this.barn, utgifter, this.aktivitetstype, this.periodetype))
        måned = måned.plusMonths(1)
    }
    return perioder
}

/**
 * Merger sammenhengende perioder hvor beløp, aktivitetstype, periodetype og
 * @BeløpsperiodeBarnetilsynDto#beregningsgrunnlag (it.toKey()) er like.
 */
fun List<BeløpsperiodeBarnetilsynDto>.mergeSammenhengendePerioder(): List<BeløpsperiodeBarnetilsynDto> {
    val sortertPåDatoListe =
        this
            .sortedBy { it.periode }
            .filter { it.periodetype == PeriodetypeBarnetilsyn.ORDINÆR }
    return sortertPåDatoListe.fold(mutableListOf()) { acc, entry ->
        val last = acc.lastOrNull()
        if (
            last != null &&
            last.hengerSammenMed(entry) &&
            last.sammeBeløpOgBeregningsgrunnlag(entry) &&
            last.sammeAktivitet(entry)
        ) {
            acc.removeLast()
            acc.add(
                last.copy(
                    periode = last.periode union entry.periode,
                ),
            )
        } else {
            acc.add(entry)
        }
        acc
    }
}

fun BeløpsperiodeBarnetilsynDto.hengerSammenMed(other: BeløpsperiodeBarnetilsynDto) = this.periode påfølgesAv other.periode

fun BeløpsperiodeBarnetilsynDto.sammeBeløpOgBeregningsgrunnlag(other: BeløpsperiodeBarnetilsynDto) =
    this.beløp == other.beløp &&
        this.beregningsgrunnlag == other.beregningsgrunnlag

fun BeløpsperiodeBarnetilsynDto.sammeAktivitet(other: BeløpsperiodeBarnetilsynDto) = this.aktivitetstype == other.aktivitetstype
