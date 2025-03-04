package no.nav.familie.ef.sak.amelding.inntektv2

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth

data class InntektV2Response(
    @JsonProperty("data")
    val maanedsData: List<MånedsInntekt> = emptyList(),
)

data class MånedsInntekt(
    val maaned: YearMonth,
    val opplysningspliktig: String,
    val underenhet: String,
    val norskident: String,
    val oppsummeringstidspunkt: OffsetDateTime,
    val inntektListe: List<Inntekt> = emptyList(),
    val forskuddstrekkListe: List<Forskuddstrekk> = emptyList(),
    val avvikListe: List<Avvik> = emptyList(),
)

data class Inntekt(
    val type: InntektTypeV2,
    val beloep: Double,
    val fordel: String,
    val beskrivelse: String,
    val inngaarIGrunnlagForTrekk: Boolean,
    val utloeserArbeidsgiveravgift: Boolean,
    val skatteOgAvgiftsregel: String?,
    val opptjeningsperiodeFom: LocalDate?,
    val opptjeningsperiodeTom: LocalDate?,
    val tilleggsinformasjon: Tilleggsinformasjon?,
    val manuellVurdering: Boolean,
    val antall: Int?,
    val skattemessigBosattLand: String?,
    val opptjeningsland: String?
)

data class Forskuddstrekk(
    val beloep: Double,
    val beskrivelse: String?
)

data class Avvik(
    val kode: String,
    val tekst: String?
)

data class Tilleggsinformasjon(
    val type: String
)

// TODO: Husk å endre tilbake til normalt navn, samt fjerne bruken et at annet sted.
enum class InntektTypeV2 {
    @JsonProperty("Loennsinntekt")
    LØNNSINNTEKT,

    @JsonProperty("Naeringsinntekt")
    NAERINGSINNTEKT,

    @JsonProperty("PensjonEllerTrygd")
    PENSJON_ELLER_TRYGD,

    @JsonProperty("YtelseFraOffentlige")
    YTELSE_FRA_OFFENTLIGE
}

fun InntektV2Response.oppsummerInntektForÅr(år: Int): Double {
    return this.maanedsData
        .filter { it.maaned.year == år }
        .flatMap { it.inntektListe }
        .sumOf { it.beloep }
}

fun List<Inntekt>.filterBasertPåInntektType(inntektType: InntektTypeV2): List<Inntekt> {
    return this.filter { it.type == inntektType }
}

