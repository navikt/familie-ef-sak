package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping(
    path = ["/api/oppgaverforbarn"],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@Unprotected
class InitOppfølgingsoppgaverForBarnTaskController(
    private val taskRepository: TaskRepository,
    private val barnFyllerÅrOppfølgingsoppgaveService: BarnFyllerÅrOppfølgingsoppgaveService
) {

    @PostMapping("/initialiser")
    fun opprettTask() {
        taskRepository.save(OpprettOppfølgingsoppgaverForBarnFyltÅrTask.opprettTask(LocalDate.now().plusDays(1)))
    }

    @PostMapping("/dry-run")
    fun dryRun(@RequestParam referansedato: LocalDate) {
        barnFyllerÅrOppfølgingsoppgaveService.opprettOppgaverForAlleBarnSomHarFyltÅr()
    }
}
