package no.nav.familie.ef.sak.sigrun.ekstern

data class SummertSkattegrunnlag(
    val grunnlag: List<Grunnlag>,
    val svalbardGrunnlag: List<Grunnlag>,
    val skatteoppgjoersdato: String,
)

data class Grunnlag(
    val tekniskNavn: String,
    val beloep: Int,
)

data class BeregnetSkatt(val tekniskNavn: String, val verdi: String)
