package no.nav.familie.ef.sak.infrastruktur.sikkerhet

enum class Rolle {
    APPLICATION,
    SAKSBEHANDLER,
    BESLUTTER,
    VEILEDER,
    FORVALTER,
    ;

    fun authority(): String = "ROLE_$name"
}
