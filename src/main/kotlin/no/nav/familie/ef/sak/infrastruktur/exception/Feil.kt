package no.nav.familie.ef.sak.infrastruktur.exception

import org.springframework.http.HttpStatus
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

data class ApiFeil(val feil: String, val httpStatus: HttpStatus) : RuntimeException(feil)

class Feil(message: String,
           val frontendFeilmelding: String? = null,
           val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
           throwable: Throwable? = null) : RuntimeException(message, throwable) {

    constructor(message: String, throwable: Throwable?, httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR) :
            this(message, null, httpStatus, throwable)
}

@OptIn(ExperimentalContracts::class)
inline fun feilHvis(boolean: Boolean, httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR, lazyMessage: () -> String) {
    contract {
        returns() implies !boolean
    }
    if (boolean) {
        throw Feil(message = lazyMessage(), frontendFeilmelding = lazyMessage(), httpStatus)
    }
}

inline fun feilHvisIkke(boolean: Boolean, httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR, lazyMessage: () -> String) {
    feilHvis(!boolean, httpStatus) { lazyMessage() }
}

class ManglerTilgang(val melding: String) : RuntimeException(melding)
