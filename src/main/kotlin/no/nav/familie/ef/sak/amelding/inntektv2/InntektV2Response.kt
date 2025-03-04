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

    @JsonProperty("Pensjon_eller_trygd")
    PENSJON_ELLER_TRYGD,

    @JsonProperty("Ytelse_fra_offentlige")
    YTELSE_FRA_OFFENTLIGE
}