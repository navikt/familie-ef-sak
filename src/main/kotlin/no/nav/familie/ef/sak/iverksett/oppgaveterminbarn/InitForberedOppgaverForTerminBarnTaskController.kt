package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping(path = ["/api/oppgaver-for-terminbarn"])
@Unprotected
class InitForberedOppgaverForTerminBarnTaskController(private val taskService: TaskService) {

    @PostMapping("/initialiser")
    fun opprettTask(): ResponseEntity<Unit> {
        taskService.save(
            Task(
                ForberedOppgaverTerminbarnTask.TYPE,
                LocalDate.now().minusWeeks(1).format(DateTimeFormatter.ISO_DATE),
            ),
        )
        return ResponseEntity(HttpStatus.OK)
    }
}
