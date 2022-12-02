package no.nav.familie.ef.sak.sigrun.ekstern

import java.time.Year

data class SummertSkattegrunnlagMap(val summertskattegrunnlagMap: Map<Year, SummertSkattegrunnlag?>)

data class SummertSkattegrunnlag(
    val grunnlag: List<Grunnlag>,
    val svalbardGrunnlag: List<Grunnlag>,
    val skatteoppgjoersdato: String
)

data class Grunnlag(
    val tekniskNavn: String,
    val beloep: Int
)

data class BeregnetSkatt(val tekniskNavn: String, val verdi: String)
