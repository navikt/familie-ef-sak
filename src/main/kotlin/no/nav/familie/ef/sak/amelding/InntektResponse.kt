package no.nav.familie.ef.sak.amelding

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth

data class InntektResponse(
    @JsonProperty("data")
    val inntektsmåneder: List<Inntektsmåned> = emptyList(),
) {
    fun totalInntektFraÅrMåned(årMåned: YearMonth): Int =
        inntektsmåneder
            .filter { it.måned.isEqualOrAfter(årMåned) && it.måned.isBefore(YearMonth.now())}
            .flatMap { it.inntektListe }
            .sumOf { it.beløp }
            .toInt()

    fun førsteMånedOgInntektMed10ProsentØkning() =
        inntektsmåneder
            .associate { it.måned to it.totalInntekt() }
            .entries
            .zipWithNext()
            .firstOrNull { it.first.value * 1.1 <= it.second.value }
            ?.second
            ?.toPair()

    fun inntektsmånderUtenEfOvergangstonad(): List<Inntektsmåned> =
        inntektsmåneder.filter { inntektsmåned ->
            inntektsmåned.inntektListe.all {
                it.type != InntektType.YTELSE_FRA_OFFENTLIGE &&
                        it.beskrivelse != "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere"
            }
        }

    fun forventetMånedsinntekt() =
        if (harTreForrigeInntektsmåneder) {
            totalInntektFraÅrMåned(YearMonth.now().minusMonths(3))/3
        } else {
            throw IllegalStateException("Mangler inntektsinformasjon for de tre siste måneder")
        }

    fun forventetÅrsinntekt() = forventetMånedsinntekt() * 12

    fun revurderesFraDato() = førsteMånedOgInntektMed10ProsentØkning()?.first

    val harTreForrigeInntektsmåneder = inntektsmåneder
        .filter { it.måned.isEqualOrAfter(YearMonth.now().minusMonths(3)) && it.måned.isBefore(YearMonth.now()) }
        .map { it.måned }
        .distinct().size == 3
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

fun List<Inntektsmåned>.summerTotalInntekt(): Double = this.flatMap { it.inntektListe }.sumOf { it.beløp }
