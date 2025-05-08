package no.nav.familie.ef.sak.brev

enum class Brevmal(
    val apiNavn: String,
    val tittel: String,
) {
    VARSEL_OM_AKTIVITETSPLIKT("varselAktivitetsplikt", "Varsel om aktivitetsplikt"),
    UKJENT("", ""),
}

// enum class Brevmal {
//    VARSEL_OM_AKTIVITETSPLIKT,
//    UKJENT,
// }
//
// fun Brevmal.tilApinavn(): String {
//    return when (this) {
//        Brevmal.VARSEL_OM_AKTIVITETSPLIKT -> "varselAktivitetsplikt"
//        Brevmal.UKJENT -> ""
//    }
// }
//
// fun Brevmal.tilTittel(): String {
//    return when (this) {
//        Brevmal.VARSEL_OM_AKTIVITETSPLIKT -> "Varsel om aktivitetsplikt"
//        Brevmal.UKJENT -> ""
//    }
// }
