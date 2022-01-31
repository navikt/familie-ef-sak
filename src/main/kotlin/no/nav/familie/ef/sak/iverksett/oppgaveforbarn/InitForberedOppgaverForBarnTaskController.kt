package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Properties

@RestController
@RequestMapping(
        path = ["/api/oppgaverforbarn"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
)
@ProtectedWithClaims(issuer = "azuread")
class InitForberedOppgaverForBarnTaskController(private val forberedOppgaverForBarnTask: ForberedOppgaverForBarnTask) {

    val DATE_FORMAT_ISO_YEAR_MONTH_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @PostMapping("/initialiser")
    fun opprettTask(): ResponseEntity<Unit> {
        forberedOppgaverForBarnTask.doTask(Task(ForberedOppgaverForBarnTask.TYPE,
                                                LocalDate.now().minusWeeks(1).format(DATE_FORMAT_ISO_YEAR_MONTH_DAY),
                                                Properties()))
        return ResponseEntity(HttpStatus.OK)
    }
}