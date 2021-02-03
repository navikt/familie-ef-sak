package no.nav.familie.ef.sak.exception

open class PdlRequestException(melding: String? = null) : Exception(melding)

class PdlNotFoundException: PdlRequestException()