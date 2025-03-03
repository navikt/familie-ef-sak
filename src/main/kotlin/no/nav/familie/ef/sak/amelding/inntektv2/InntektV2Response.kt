package no.nav.familie.ef.sak.amelding.inntektv2

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ef.sak.amelding.ekstern.InntektType
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth

data class InntektV2Response(
    @JsonProperty("data")
    val maanedsData: List<MaanedsData>
)

data class MaanedsData(
    val maaned: YearMonth,
    val opplysningspliktig: String,
    val underenhet: String,
    val norskident: String,
    val oppsummeringstidspunkt: OffsetDateTime,
    val inntektListe: List<InntektV2>,
    val forskuddstrekkListe: List<ForskuddstrekkV2>,
    val avvikListe: List<AvvikV2>
)

data class InntektV2(
    val type: InntektType,
    val beloep: Double,
    val fordel: String,
    val beskrivelse: String,
    val inngaarIGrunnlagForTrekk: Boolean,
    val utloeserArbeidsgiveravgift: Boolean,
    val skatteOgAvgiftsregel: String,
    val opptjeningsperiodeFom: LocalDate,
    val opptjeningsperiodeTom: LocalDate,
    val tilleggsinformasjon: TilleggsinformasjonV2?,
    val manuellVurdering: Boolean,
    val antall: Int?,
    val skattemessigBosattLand: String?,
    val opptjeningsland: String?
)

data class ForskuddstrekkV2(
    val beloep: Double,
    val beskrivelse: String?
)

data class AvvikV2(
    val kode: String,
    val tekst: String?
)

data class TilleggsinformasjonV2(
    val type: String
)