package no.nav.familie.ef.sak.inntekt

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
        val arbeidsforholdListe: List<ArbeidsforholdFrilanser>?, // mulig man bare burde si ifra om att X har arbeidsforhold som frilanser uten noen detaljer hvis det er relevant for saksbehandlere
        val inntektListe: List<Inntekt>?,
)

data class ArbeidsforholdFrilanser(
        val antallTimerPerUkeSomEnFullStillingTilsvarer: Double? = null,
        val arbeidstidsordning: String? = null,
        val avloenningstype: String? = null,
        val sisteDatoForStillingsprosentendring: LocalDate? = null,
        val sisteLoennsendring: LocalDate? = null,
        val frilansPeriodeFom: LocalDate? = null,
        val frilansPeriodeTom: LocalDate? = null,
        val stillingsprosent: Double? = null,
        val yrke: String? = null,
        val arbeidsforholdID: String? = null,
        val arbeidsforholdIDnav: String? = null,
        val arbeidsforholdstype: String? = null,
        val arbeidsgiver: Aktoer? = null,
        val arbeidstaker: Aktoer? = null
)

data class Inntekt(
        val inntektType: InntektType,
        val beloep: Int,
        val fordel: String? = null, //kontantytese / etc
        val opptjeningsland: String? = null,
        val opptjeningsperiodeFom: LocalDate? = null,
        val opptjeningsperiodeTom: LocalDate? = null,
        val skattemessigBosattLand: String? = null,
        val virksomhet: Aktoer,//? = null,
        val tilleggsinformasjon: Tilleggsinformasjon? = null,
        val beskrivelse: String? = null, // hentes fra kodeverk
)

data class Tilleggsinformasjon(
        val kategori: String? = null, //  KodeverkEDAGTilleggsinfoKategorier
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

enum class TilleggsinformasjonDetaljerType {
    ALDERSUFOEREETTERLATTEAVTALEFESTETOGKRIGSPENSJON,
    BARNEPENSJONOGUNDERHOLDSBIDRAG,
    BONUSFRAFORSVARET,
    ETTERBETALINGSPERIODE,
    INNTJENINGSFORHOLD,
    REISEKOSTOGLOSJI,
    SVALBARDINNTEKT
}
