package no.nav.familie.ef.sak.util

import org.springframework.http.HttpStatus

open class Feil(message: String,
           open val frontendFeilmelding: String? = null,
           open val httpStatus: HttpStatus = HttpStatus.OK,
           open val throwable: Throwable? = null) : RuntimeException(message)
