package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.iverksett.oppgaveterminbarn.ForberedOppgaverTerminbarnTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping(path = ["/api/oppgaver-for-terminbarn"])
@ProtectedWithClaims(issuer = "azuread")
class InitForberedOppgaverForTerminBarnTaskController(
    private val taskService: TaskService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("/initialiser")
    fun opprettTask(): ResponseEntity<Unit> {
        tilgangService.validerHarForvalterrolle()
        taskService.save(
            Task(
                ForberedOppgaverTerminbarnTask.TYPE,
                LocalDate.now().minusWeeks(1).format(DateTimeFormatter.ISO_DATE),
            ),
        )
        return ResponseEntity(HttpStatus.OK)
    }
}
