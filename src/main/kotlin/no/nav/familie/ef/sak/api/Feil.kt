package no.nav.familie.ef.sak.api

import org.springframework.http.HttpStatus

data class ApiFeil(val feil: String, val httpStatus: HttpStatus) : RuntimeException()

class Feil(message: String,
           val frontendFeilmelding: String? = null,
           val httpStatus: HttpStatus = HttpStatus.OK,
           throwable: Throwable? = null) : RuntimeException(message, throwable)