package no.nav.familie.ef.sak.amelding

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth

data class InntektResponse(
    @JsonProperty("data")
    val inntektsMåneder: List<InntektMåned> = emptyList(),
)

data class InntektMåned(
    @JsonProperty("maaned")
    val måned: YearMonth,
    val opplysningspliktig: String,
    val underenhet: String,
    val norskident: String,
    @JsonProperty("oppsummeringstidspunkt")
    val oppsummeringsTidspunkt: OffsetDateTime,
    val inntektListe: List<Inntekt> = emptyList(),
    val forskuddstrekkListe: List<Forskuddstrekk> = emptyList(),
    val avvikListe: List<Avvik> = emptyList(),
)

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

fun InntektResponse.oppsummerInntektForÅr(år: Int): Double =
    this.inntektsMåneder
        .filter { it.måned.year == år }
        .flatMap { it.inntektListe }
        .sumOf { it.beløp }

fun List<Inntekt>.filterBasertPåInntektType(inntektType: InntektType): List<Inntekt> = this.filter { it.type == inntektType }

fun List<InntektMåned>.summerTotalInntekt(): Double = this.flatMap { it.inntektListe }.sumOf { it.beløp }
