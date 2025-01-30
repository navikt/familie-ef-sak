package no.nav.familie.ef.sak.selvstendig

data class NæringsinntektIngenEndringPdfRequest(
    val saksid: String,
    val personIdent: String,
    val navn: String,
    val saksbehandlernavn: String,
    val enhet: String,
    val forventetInntekt: Int,
    val næringsinntekt: Int,
    val personinntekt: Int,
    val årstall: Int,
)
