package no.nav.familie.ef.sak.brev

enum class Brevmal(
    val apiNavn: String,
    val tittel: String,
) {
    VARSEL_OM_AKTIVITETSPLIKT("varselAktivitetsplikt", "Varsel om aktivitetsplikt"),
}
