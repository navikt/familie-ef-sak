package no.nav.familie.ef.sak.inntekt.ekstern

import java.time.LocalDate
import java.time.YearMonth

data class HentInntektListeResponse(
        val arbeidsInntektMaaned: List<ArbeidsInntektMaaned>?,
        val ident: Aktoer
)

data class ArbeidsInntektMaaned(
        val aarMaaned: YearMonth,// rapportert for den måneden
        val avvikListe: List<Avvik>?,
        val arbeidsInntektInformasjon: ArbeidsInntektInformasjon?
)

data class Aktoer(
        val identifikator: String,
        val aktoerType: AktoerType
)

data class Avvik(
        val ident: Aktoer? = null,
        val opplysningspliktig: Aktoer? = null,
        val virksomhet: Aktoer,
        val avvikPeriode: YearMonth? = null,
        val tekst: String? = null
)

data class ArbeidsInntektInformasjon(
        val inntektListe: List<Inntekt>?,
)

data class Inntekt(
        val inntektType: InntektType,
        val beloep: Int,
        val fordel: String, //kontantytese / etc
        val opptjeningsland: String? = null,
        val opptjeningsperiodeFom: LocalDate? = null,
        val opptjeningsperiodeTom: LocalDate? = null,
        val skattemessigBosattLand: String? = null,
        val virksomhet: Aktoer,//? = null,
        val tilleggsinformasjon: Tilleggsinformasjon? = null,
        val beskrivelse: String? = null, // hentes fra kodeverk
)

data class Tilleggsinformasjon(
        val kategori: String? = null, // Kodeverk -> EDAGTilleggsinfoKategorier
)

enum class AktoerType {
    AKTOER_ID,
    NATURLIG_IDENT,
    ORGANISASJON,
}

enum class InntektType {
    LOENNSINNTEKT,
    NAERINGSINNTEKT,
    PENSJON_ELLER_TRYGD,
    YTELSE_FRA_OFFENTLIGE
}
