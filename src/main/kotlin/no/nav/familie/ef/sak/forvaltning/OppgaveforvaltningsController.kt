package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/oppgave/forvaltning/")
@ProtectedWithClaims(issuer = "azuread")
class OppgaveforvaltningsController(
    private val taskService: TaskService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("behandling/{behandlingId}")
    fun loggOppgavemetadataFor(@PathVariable behandlingId: UUID) {
        feilHvisIkke(tilgangService.harForvalterrolle()) { "Må være forvalter for å bruke forvaltningsendepunkt" }
        val task = LoggOppgaveMetadataTask.opprettTask(behandlingId)
        taskService.save(task)
    }
}
