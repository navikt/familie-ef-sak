package no.nav.familie.ef.sak.brev

enum class Brevmal(
    val apiNavn: String,
    val visningsnavn: String,
) {
    VARSEL_AKTIVITETSPLIKT("varselAktivitetsplikt", "Varsel om aktivitetsplikt"),
    UKJENT("", ""),
}
