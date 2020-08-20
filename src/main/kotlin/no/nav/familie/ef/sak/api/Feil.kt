package no.nav.familie.ef.sak.api

import org.springframework.http.HttpStatus

data class ApiFeil(val feil: String, val httpStatus: HttpStatus): RuntimeException()

class Feil : RuntimeException {
    val frontendFeilmelding: String?
    val httpStatus: HttpStatus

    constructor(message: String,
                frontendFeilmelding: String? = null,
                httpStatus: HttpStatus = HttpStatus.OK,
                throwable: Throwable? = null) : super(message, throwable) {
        this.frontendFeilmelding = frontendFeilmelding
        this.httpStatus = httpStatus
    }
}
