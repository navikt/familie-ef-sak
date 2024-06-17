package no.nav.familie.ef.sak.sigrun.ekstern

import java.time.LocalDate

data class PensjonsgivendeInntektResponse(
    val norskPersonidentifikator: String?, // Kan bli null dersom person ikke finnes
    val inntektsaar: Int?,
    val pensjonsgivendeInntekt: List<PensjonsgivendeInntektForSkatteordning>?,
)

data class PensjonsgivendeInntektForSkatteordning(
    val skatteordning: Skatteordning,
    val datoForFastsetting: LocalDate,
    val pensjonsgivendeInntektAvLoennsinntekt: Int?,
    val pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel: Int?,
    val pensjonsgivendeInntektAvNaeringsinntekt: Int?,
    val pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage: Int?,
)

enum class Skatteordning {
    FASTLAND,
    SVALBARD,
    KILDESKATT_PAA_LOENN,
}

data class SummertSkattegrunnlag(
    val grunnlag: List<Grunnlag>,
    val svalbardGrunnlag: List<Grunnlag>,
    val skatteoppgjoersdato: String,
)

data class Grunnlag(
    val tekniskNavn: String,
    val beloep: Int,
)

data class BeregnetSkatt(
    val tekniskNavn: String,
    val verdi: String,
)

/*
{
  "norskPersonidentifikator": "09528731462",
  "inntektsaar": 2021,
  "pensjonsgivendeInntekt": [
    {
      "skatteordning": "FASTLAND",
      "datoForFastsetting": "2022-09-27",
      "pensjonsgivendeInntektAvLoennsinntekt": 698219,
      "pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel": null,
      "pensjonsgivendeInntektAvNaeringsinntekt": 150000,
      "pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage": 85000
    },
    {
      "skatteordning": "SVALBARD",
      "datoForFastsetting": "2022-09-27",
      "pensjonsgivendeInntektAvLoennsinntekt": 492160,
      "pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel": null,
      "pensjonsgivendeInntektAvNaeringsinntekt": 2530000,
      "pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage": null
    }
  ]
}

 */
