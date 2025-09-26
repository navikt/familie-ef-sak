package no.nav.familie.ef.sak.amelding

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder.finnGrunnbeløp
import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.felles.util.isEqualOrBefore
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import kotlin.math.abs

data class InntektResponse(
    @JsonProperty("data")
    val inntektsmåneder: List<Inntektsmåned> = emptyList(),
) {
    private val cutoffYearMonth = if (skalMedberegneInntektFraInneværendeMåned()) YearMonth.now() else YearMonth.now().minusMonths(1)

    fun totalInntektFraÅrMåned(
        årMåned: YearMonth,
        tilÅrMåned: YearMonth? = null,
    ): Int =
        inntektsmånederFraOgMedÅrMåned(årMåned, tilÅrMåned)
            .filter { it.måned.isEqualOrAfter(årMåned) && it.måned.isEqualOrBefore(tilÅrMåned ?: cutoffYearMonth) }
            .flatMap { it.inntektListe }
            .filterNot { ignorerteYtelserOgUtbetalinger.contains(it.beskrivelse) }
            .sumOf { it.beløp }
            .toInt()

    fun totalInntektForMånedsperiodeUtenFeriepengerOgHelligdagstillegg(fraOgMedÅrMåned: YearMonth): Int =
        inntektsmånederFraOgMedÅrMåned(fraOgMedÅrMåned)
            .filter { it.måned.isEqualOrAfter(fraOgMedÅrMåned) && it.måned.isEqualOrBefore(cutoffYearMonth) }
            .flatMap { it.inntektListe }
            .filterNot { ignorerteYtelserOgUtbetalinger.contains(it.beskrivelse) || it.beskrivelse.contains("ferie", true) || it.beskrivelse == "helligdagstillegg" }
            .sumOf { it.beløp }
            .toInt()

    fun antallMånederUtenFeriepenger(fraOgMedÅrMåned: YearMonth): Int =
        inntektsmånederFraOgMedÅrMåned(fraOgMedÅrMåned)
            .filter { it.måned.isEqualOrAfter(fraOgMedÅrMåned) && it.måned.isEqualOrBefore(cutoffYearMonth) }
            .filterNot { it.inntektListe.all { it.beskrivelse.contains("ferie", true) || ignorerteYtelserOgUtbetalinger.contains(it.beskrivelse) } }
            .groupBy { it.måned }
            .count()

    fun harMånedMedBareFeriepenger(fraOgMedÅrMåned: YearMonth): Boolean = antallMånederUtenFeriepenger(fraOgMedÅrMåned) < 3

    fun skalMedberegneInntektFraInneværendeMåned() =
        inntektsmåneder
            .any { inntektsmåned ->
                inntektsmåned.måned.equals(YearMonth.now())
            } &&
            harIkkeAndreNavYtelser(YearMonth.now().minusMonths(3)) &&
            ikkeForStortAvvikMellomInneværendemånedOgForrigeMånedBeløp() &&
            harKunEnArbeidsgiver(YearMonth.now().minusMonths(3))

    fun finnesFeriepengerFraOgMedÅrMåned(fraOgMedÅrMåned: YearMonth): Boolean =
        inntektsmånederFraOgMedÅrMåned(fraOgMedÅrMåned)
            .filter { it.måned.isEqualOrAfter(fraOgMedÅrMåned) && it.måned.isEqualOrBefore(cutoffYearMonth) }
            .any { it.inntektListe.any { it.beskrivelse.contains("ferie", true) } }

    fun harIkkeAndreNavYtelser(fraogMedÅr: YearMonth): Boolean = inntektsmånederFraOgMedÅrMåned(fraogMedÅr, YearMonth.now()).none { it.inntektListe.any { it.type == InntektType.YTELSE_FRA_OFFENTLIGE && !it.beskrivelse.equals("overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere") } }

    fun ikkeForStortAvvikMellomInneværendemånedOgForrigeMånedBeløp(): Boolean {
        val snittInntekt = totalInntektFraÅrMåned(YearMonth.now().minusMonths(1), YearMonth.now().minusMonths(1))
        val inneværendeMånedsInntekt = totalInntektFraÅrMåned(YearMonth.now(), YearMonth.now())
        return abs(snittInntekt - inneværendeMånedsInntekt) < 3000
    }

    fun harKunEnArbeidsgiver(fraOgMedÅr: YearMonth): Boolean =
        inntektsmånederFraOgMedÅrMåned(fraOgMedÅr, YearMonth.now())
            .filter { it.måned.isEqualOrAfter(fraOgMedÅr) }
            .map { Pair(it.underenhet, it.inntektListe) }
            .filterNot { it.second.any { it.beskrivelse.equals("overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere") } }
            .groupBy { it.first }
            .size <= 1

    fun totalInntektForÅrMåned(årMåned: YearMonth): Int =
        inntektsmånederFraOgMedÅrMåned(årMåned)
            .filter { it.måned == årMåned }
            .flatMap { it.inntektListe }
            .filterNot { ignorerteYtelserOgUtbetalinger.contains(it.beskrivelse) }
            .sumOf { it.beløp }
            .toInt()

    fun totalInntektForÅrMånedUtenFeriepenger(årMåned: YearMonth): Int =
        inntektsmånederFraOgMedÅrMåned(årMåned)
            .filter { it.måned == årMåned }
            .flatMap { it.inntektListe }
            .filterNot { ignorerteYtelserOgUtbetalinger.contains(it.beskrivelse) || it.beskrivelse.contains("ferie", true) }
            .sumOf { it.beløp }
            .toInt()

    fun mapTilSummertInntekt(forrigeVedtak: Vedtak): List<SummertInntekt> =
        inntektsmåneder
            .sortedBy { it.måned }
            .filter {
                it.måned.isEqualOrBefore(cutoffYearMonth) &&
                    it.måned.isEqualOrAfter(
                        forrigeVedtak.perioder
                            ?.perioder
                            ?.first()
                            ?.periode
                            ?.fom,
                    )
            }.map {
                SummertInntekt(
                    it.måned,
                    totalInntektForÅrMånedUtenFeriepenger(it.måned),
                    forrigeVedtak.inntekter
                        ?.inntekter
                        ?.first { vedtak -> vedtak.periode.inneholder(it.måned) }
                        ?.avledForventetMånedsinntekt() ?: throw IllegalStateException("Burde hatt forventet inntekt for årMåned: ${it.måned} for vedtak: $forrigeVedtak"),
                )
            }

    fun førsteMånedMed10ProsentInntektsøkning(forrigeVedtak: Vedtak): YearMonth {
        val summertInntektList = mapTilSummertInntekt(forrigeVedtak)

        return summertInntektList.indices
            .asSequence()
            .firstOrNull { i ->
                val dropped = summertInntektList.drop(i)
                dropped.take(2).all { it.innmeldtInntekt >= it.forventetInntekt * 1.1 } &&
                    dropped.all { it.innmeldtInntekt > finnGrunnbeløp(it.årMåned).perMnd.toInt() / 2 && it.innmeldtInntekt >= it.forventetInntekt }
            }?.let { summertInntektList[it].årMåned } ?: throw IllegalStateException("Burde funnet måned med 10% inntekt for behandling: ${forrigeVedtak.behandlingId}")
    }

    fun inntektsmånederFraOgMedÅrMåned(
        fraOgMedÅrMåned: YearMonth? = null,
        tilOgMedÅrMåned: YearMonth? = null,
    ): List<Inntektsmåned> =
        inntektsmåneder
            .filter { inntektsmåned ->
                inntektsmåned.måned.isEqualOrBefore(tilOgMedÅrMåned ?: cutoffYearMonth) &&
                    inntektsmåned.måned.isEqualOrAfter(fraOgMedÅrMåned)
            }.sortedBy { it.måned }

    fun forventetMånedsinntekt(): Int {
        if (!harTreForrigeInntektsmåneder) {
            throw IllegalStateException("Mangler inntektsinformasjon for de tre siste måneder")
        }

        return totalInntektForMånedsperiodeUtenFeriepengerOgHelligdagstillegg(cutoffYearMonth.minusMonths(2)) / 3
    }

    val harTreForrigeInntektsmåneder =
        inntektsmåneder
            .filter { it.måned.isEqualOrAfter(YearMonth.now().minusMonths(3)) && it.måned.isBefore(YearMonth.now()) }
            .distinctBy { it.måned }
            .size == 3

    val finnesHøyMånedsinntektSomIkkeGirOvergangsstønad =
        inntektsmåneder.any { totalInntektForÅrMåned(it.måned) * 12 > finnGrunnbeløp(it.måned).grunnbeløp.toInt() * 5.5 }

    companion object {
        private val ignorerteYtelserOgUtbetalinger = listOf("overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere", "barnepensjon", "introduksjonsstoenad")
    }
}

data class Inntektsmåned(
    @JsonProperty("maaned")
    val måned: YearMonth,
    val opplysningspliktig: String,
    val underenhet: String,
    val norskident: String,
    val oppsummeringstidspunkt: OffsetDateTime,
    val inntektListe: List<Inntekt> = emptyList(),
    val forskuddstrekkListe: List<Forskuddstrekk> = emptyList(),
    val avvikListe: List<Avvik> = emptyList(),
) {
    fun totalInntekt() = inntektListe.sumOf { it.beløp }
}

data class Inntekt(
    val type: InntektType,
    @JsonProperty("beloep")
    val beløp: Double,
    val fordel: String,
    val beskrivelse: String,
    @JsonProperty("inngaarIGrunnlagForTrekk")
    val inngårIGrunnlagForTrekk: Boolean,
    @JsonProperty("utloeserArbeidsgiveravgift")
    val utløserArbeidsgiveravgift: Boolean,
    val skatteOgAvgiftsregel: String?,
    val opptjeningsperiodeFom: LocalDate?,
    val opptjeningsperiodeTom: LocalDate?,
    val tilleggsinformasjon: Tilleggsinformasjon?,
    val manuellVurdering: Boolean,
    val antall: Int?,
    val skattemessigBosattLand: String?,
    val opptjeningsland: String?,
)

data class Forskuddstrekk(
    @JsonProperty("beloep")
    val beløp: Double,
    val beskrivelse: String?,
)

data class Avvik(
    val kode: String,
    val tekst: String?,
)

data class Tilleggsinformasjon(
    val type: String,
)

data class InntektRequestBody(
    val månedFom: YearMonth?,
    val månedTom: YearMonth?,
)

data class HentInntektPayload(
    val personIdent: String,
    val månedFom: YearMonth,
    val månedTom: YearMonth,
)

enum class InntektType {
    @JsonProperty("Loennsinntekt")
    LØNNSINNTEKT,

    @JsonProperty("Naeringsinntekt")
    NAERINGSINNTEKT,

    @JsonProperty("PensjonEllerTrygd")
    PENSJON_ELLER_TRYGD,

    @JsonProperty("YtelseFraOffentlige")
    YTELSE_FRA_OFFENTLIGE,
}

data class SummertInntekt(
    val årMåned: YearMonth,
    val innmeldtInntekt: Int,
    val forventetInntekt: Int,
)

fun List<Inntektsmåned>.summerTotalInntekt(): Double = this.flatMap { it.inntektListe }.sumOf { it.beløp }
