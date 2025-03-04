package no.nav.familie.ef.sak.amelding

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.YearMonth

data class AMeldingInntektDto(
    val inntektPerVirksomhet: List<InntektForVirksomhetDto>,
    val avvik: List<String>,
)

data class InntektForVirksomhetDto(
    val identifikator: String,
    val navn: String,
    val inntektPerMåned: Map<YearMonth, InntektPerMånedDto>,
)

data class InntektPerMånedDto(
    val totalbeløp: Int,
    val inntekt: List<InntektDto>,
)

/**
 * beskrivelse hentes fra kodeverk
 *  (InntektType -> Navn på kodeverk)
 *  Lønnsinntekt -> Loennsbeskrivelse
 *  Næringsinntekt -> Naeringsinntektsbeskrivelse
 *  Pensjon eller trygd -> PensjonEllerTrygdeBeskrivelse
 *  Ytelse fra offentlig -> YtelseFraOffentligeBeskrivelse
 *
 *  kategori hentes fra kodeverk - EDAGTilleggsinfoKategorier
 */
data class InntektDto(
    val beløp: Int,
    val beskrivelse: String?,
    val fordel: Fordel,
    val type: InntektType,
    val kategori: String?,
    val opptjeningsland: String? = null,
    val opptjeningsperiodeFom: LocalDate? = null,
    val opptjeningsperiodeTom: LocalDate? = null,
)

enum class InntektType {
                       @JsonProperty("")
    LØNNSINNTEKT,
    NÆRINGSINNTEKT,
    PENSJON_ELLER_TRYGD,
    YTELSE_FRA_OFFENTLIGE,
}

enum class Fordel(
    val verdi: String,
) {
    KONTANTYTELSE("kontantytelse"),
    NATURALYTELSE("naturalytelse"),
    UTGIFTSGODTGJØRELSE("utgiftsgodtgjoerelse"),
    ;

    companion object {
        val map = values().associateBy { it.verdi }

        fun fraVerdi(verdi: String): Fordel =
            map[verdi]
                ?: error("Finner ikke mapping for $verdi i ${Fordel::class.java.simpleName}")
    }
}
