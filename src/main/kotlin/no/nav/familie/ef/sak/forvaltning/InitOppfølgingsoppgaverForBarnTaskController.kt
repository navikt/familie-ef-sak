package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.OppfølgingOppgaveBarnFyllerÅrService
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.OpprettTasksForBarnFyltÅrTask
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping(
    path = ["/api/oppgaverforbarn"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class InitOppfølgingsoppgaverForBarnTaskController(
    private val taskService: TaskService,
    private val barnFyllerÅrOppfølgingsoppgaveService: OppfølgingOppgaveBarnFyllerÅrService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("/initialiser")
    fun opprettTask() {
        tilgangService.validerHarForvalterrolle()
        taskService.save(OpprettTasksForBarnFyltÅrTask.opprettTask(LocalDate.now().plusDays(1)))
    }

    @PostMapping("/dry-run")
    fun dryRun() {
        tilgangService.validerHarForvalterrolle()
        barnFyllerÅrOppfølgingsoppgaveService.opprettTasksForAlleBarnSomHarFyltÅr(dryRun = true)
    }
}
