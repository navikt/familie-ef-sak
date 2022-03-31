package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.ForberedOppgaverForBarnTask
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
@RequestMapping(path = ["/api/oppgaverforterminbarn"])
@ProtectedWithClaims(issuer = "azuread")
class InitForberedOppgaverForBarnTaskController(private val forberedOppgaverTerminbarnTask: ForberedOppgaverTerminbarnTask) {

    @PostMapping("/initialiser")
    fun opprettTask(): ResponseEntity<Unit> {
        forberedOppgaverTerminbarnTask.doTask(Task(ForberedOppgaverForBarnTask.TYPE,
                                                   LocalDate.now().minusWeeks(1).format(DateTimeFormatter.ISO_DATE),
                                                   Properties()))
        return ResponseEntity(HttpStatus.OK)
    }
}