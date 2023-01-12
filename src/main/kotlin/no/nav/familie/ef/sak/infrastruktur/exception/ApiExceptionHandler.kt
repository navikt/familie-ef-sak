package no.nav.familie.ef.sak.infrastruktur.exception

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.exceptions.JwtTokenMissingException
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

@Suppress("unused")
@ControllerAdvice
class ApiExceptionHandler(val featureToggleService: FeatureToggleService) {

    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ResponseEntity<Ressurs<Nothing>> {
        val metodeSomFeiler = finnMetodeSomFeiler(throwable)

        val mostSpecificCause = throwable.getMostSpecificCause()

        if (mostSpecificCause is SocketTimeoutException || mostSpecificCause is TimeoutException){
            logger.warn("Treff på SocketTimeoutException. Message: ${mostSpecificCause.message}")
            secureLogger.warn("Timeout feil: $metodeSomFeiler ${rootCause(throwable)}", throwable)
            logger.warn("Timeout feil: $metodeSomFeiler ${rootCause(throwable)} ")
            if(featureToggleService.isEnabled(Toggle.LOGG_WARN_TIMEOUTS)){
                 return ResponseEntity
                     .status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body(Ressurs.failure(errorMessage = "Timeout feil", frontendFeilmelding = "Kommunikasjonsproblemer med andre systemer - prøv igjen senere"))
            }

        }

        secureLogger.error("Uventet feil: $metodeSomFeiler ${rootCause(throwable)}", throwable)
        logger.error("Uventet feil: $metodeSomFeiler ${rootCause(throwable)} ")

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Ressurs.failure(errorMessage = "Uventet feil", frontendFeilmelding = "En uventet feil oppstod."))
    }

    @ExceptionHandler(JwtTokenMissingException::class)
    fun handleJwtTokenMissingException(jwtTokenMissingException: JwtTokenMissingException): ResponseEntity<Ressurs<Nothing>> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                Ressurs.failure(
                    errorMessage = "401 Unauthorized JwtTokenMissingException",
                    frontendFeilmelding = "En uventet feil oppstod: Kall ikke autorisert"
                )
            )
    }

    @ExceptionHandler(ApiFeil::class)
    fun handleThrowable(feil: ApiFeil): ResponseEntity<Ressurs<Nothing>> {
        val metodeSomFeiler = finnMetodeSomFeiler(feil)
        secureLogger.info("En håndtert feil har oppstått(${feil.httpStatus}): ${feil.feil}", feil)
        logger.info(
            "En håndtert feil har oppstått(${feil.httpStatus}) metode=$metodeSomFeiler exception=${
            rootCause(
                feil
            )
            }: ${feil.message} "
        )
        return ResponseEntity.status(feil.httpStatus).body(
            Ressurs.funksjonellFeil(
                frontendFeilmelding = feil.feil,
                melding = feil.feil
            )
        )
    }

    @ExceptionHandler(Feil::class)
    fun handleThrowable(feil: Feil): ResponseEntity<Ressurs<Nothing>> {
        val metodeSomFeiler = finnMetodeSomFeiler(feil)
        secureLogger.error("En håndtert feil har oppstått(${feil.httpStatus}): ${feil.frontendFeilmelding}", feil)
        logger.error(
            "En håndtert feil har oppstått(${feil.httpStatus}) metode=$metodeSomFeiler exception=${
            rootCause(
                feil
            )
            }: ${feil.message} "
        )
        return ResponseEntity.status(feil.httpStatus)
            .body(Ressurs.failure(frontendFeilmelding = feil.frontendFeilmelding))
    }

    @ExceptionHandler(PdlNotFoundException::class)
    fun handleThrowable(feil: PdlNotFoundException): ResponseEntity<Ressurs<Nothing>> {
        logger.warn("Finner ikke personen i PDL")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(
                Ressurs.funksjonellFeil(
                    frontendFeilmelding = "Finner ingen personer for valgt personident",
                    melding = "Finner ingen personer for valgt personident"
                )
            )
    }

    @ExceptionHandler(ManglerTilgang::class)
    fun handleThrowable(manglerTilgang: ManglerTilgang): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.warn("En håndtert tilgangsfeil har oppstått - ${manglerTilgang.melding}", manglerTilgang)
        logger.warn("En håndtert tilgangsfeil har oppstått")
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(
                Ressurs(
                    data = null,
                    status = Ressurs.Status.IKKE_TILGANG,
                    frontendFeilmelding = manglerTilgang.frontendFeilmelding,
                    melding = manglerTilgang.melding,
                    stacktrace = null
                )
            )
    }

    @ExceptionHandler(IntegrasjonException::class)
    fun handleThrowable(feil: IntegrasjonException): ResponseEntity<Ressurs<Nothing>> {
        secureLogger.error("Feil mot integrasjonsclienten har oppstått: uri={} data={}", feil.uri, feil.data, feil)
        logger.error("Feil mot integrasjonsclienten har oppstått exception=${rootCause(feil)}")
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Ressurs.failure(frontendFeilmelding = feil.message))
    }

    fun finnMetodeSomFeiler(e: Throwable): String {
        val firstElement = e.stackTrace.firstOrNull {
            it.className.startsWith("no.nav.familie.ef.sak") &&
                !it.className.contains("$") &&
                !it.className.contains("InsertUpdateRepositoryImpl")
        }
        if (firstElement != null) {
            val className = firstElement.className.split(".").lastOrNull()
            return "$className::${firstElement.methodName}(${firstElement.lineNumber})"
        }
        return e.cause?.let { finnMetodeSomFeiler(it) } ?: "(Ukjent metode som feiler)"
    }

    private fun rootCause(throwable: Throwable): String {
        return throwable.getMostSpecificCause().javaClass.simpleName
    }

    private fun Throwable.getMostSpecificCause(): Throwable {
        return NestedExceptionUtils.getMostSpecificCause(this)
    }

}


