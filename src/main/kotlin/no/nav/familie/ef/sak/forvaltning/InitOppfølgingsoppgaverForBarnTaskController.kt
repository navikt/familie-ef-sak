package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.HarRolleForvalter
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
    @HarRolleForvalter
    @PostMapping("/initialiser")
    fun opprettTask() {
        taskService.save(OpprettTasksForBarnFyltÅrTask.opprettTask(LocalDate.now().plusDays(1)))
    }

    @HarRolleForvalter
    @PostMapping("/dry-run")
    fun dryRun() {
        barnFyllerÅrOppfølgingsoppgaveService.opprettTasksForAlleBarnSomHarFyltÅr(dryRun = true)
    }
}
