package no.nav.familie.ef.sak.amelding.inntektv2

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ef.sak.amelding.ekstern.InntektType
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth

data class InntektV2Response(
    @JsonProperty("data")
    val maanedsData: List<MånedsInntekt>
)

data class MånedsInntekt(
    val maaned: YearMonth,
    val opplysningspliktig: String,
    val underenhet: String,
    val norskident: String,
    val oppsummeringstidspunkt: OffsetDateTime,
    val inntektListe: List<Inntekt>,
    val forskuddstrekkListe: List<Forskuddstrekk>,
    val avvikListe: List<Avvik>
)

data class Inntekt(
    val type: InntektType,
    val beloep: Double,
    val fordel: String,
    val beskrivelse: String,
    val inngaarIGrunnlagForTrekk: Boolean,
    val utloeserArbeidsgiveravgift: Boolean,
    val skatteOgAvgiftsregel: String,
    val opptjeningsperiodeFom: LocalDate,
    val opptjeningsperiodeTom: LocalDate,
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