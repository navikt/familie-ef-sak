package no.nav.familie.ef.sak.infrastruktur.exception

open class PdlRequestException(melding: String? = null) : Exception(melding)

class PdlNotFoundException: PdlRequestException()