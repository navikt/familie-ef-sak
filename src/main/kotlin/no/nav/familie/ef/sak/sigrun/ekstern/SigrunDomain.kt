package no.nav.familie.ef.sak.sigrun.ekstern

import java.time.Year

class SummertSkattegrunnlagMap(val summertSkattegrunnlagMap: Map<Year, SummertSkattegrunnlag?>)

class SummertSkattegrunnlag(
    val grunnlag: Grunnlag,
    val svalbardGrunnlag: Grunnlag,
    val skatteoppgjoersdato: String
)

class Grunnlag(
    val tekniskNavn: String,
    val beloep: String
)

class BeregnetSkatt(val tekniskNavn: String, val verdi: String)
