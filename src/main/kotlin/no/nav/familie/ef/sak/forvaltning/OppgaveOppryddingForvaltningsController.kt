package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/oppgave/forvaltning/opprydding")
@ProtectedWithClaims(issuer = "azuread")
class OppgaveOppryddingForvaltningsController(
    private val tilgangService: TilgangService,
    private val taskService: TaskService,
) {
    private val logger = Logg.getLogger(this::class)

    @PostMapping("start-opprydding")
    fun ryddOppgaver(
        @RequestBody runType: RunType,
    ) {
        logger.info("Starter opprydding av oppgaver")
        tilgangService.validerHarForvalterrolle()
        val task = taskService.save(OppgaveOppryddingForvaltningsTask.opprettTask(runType))

        logger.info("Opprettet task for opprydding av oppfølgingsoppgaver: ${task.id}")
    }

    data class RunType(
        val liveRun: Boolean,
    )
}
