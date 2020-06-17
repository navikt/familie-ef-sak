package no.nav.familie.ef.sak.api

import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler


@Suppress("unused")
@ControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("En feil har oppstått", throwable)
        logger.error("En feil har oppstått: ${throwable.javaClass.simpleName} ")

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Ressurs.failure("Uventet feil"))
    }

    @ExceptionHandler(Feil::class)
    fun handleThrowable(feil: Feil): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("En håndtert feil har oppstått(${feil.httpStatus}): ${feil.frontendFeilmelding}", feil)
        logger.info("En håndtert feil har oppstått(${feil.httpStatus}): ${feil.message} ")
        return ResponseEntity.status(feil.httpStatus).body(Ressurs.failure(frontendFeilmelding = feil.frontendFeilmelding))
    }

}