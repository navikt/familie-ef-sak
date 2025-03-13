package no.nav.familie.ef.sak.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class ForvaltningFerdigstillRequest(
    val behandlingId: UUID,
    val oppgavetype: Oppgavetype,
)

@RestController
@RequestMapping("/api/oppgave/forvaltning/")
@ProtectedWithClaims(issuer = "azuread")
class OppgaveforvaltningsController(
    private val taskService: TaskService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("behandling/{behandlingId}")
    fun loggOppgavemetadataFor(
        @PathVariable behandlingId: UUID,
    ) {
        tilgangService.validerHarForvalterrolle()
        val task = LoggOppgaveMetadataTask.opprettTask(behandlingId)
        taskService.save(task)
    }

    @Operation(
        description =
            "Hvis en åpen behandling mangler oppgave kan vi bruke dette endepunktet for å lage en ny oppgave på behandlingen. \n\n" +
                "Sjekk først om det finnes en oppgave for behandlingen. Dette kan du gjøre med forvaltningsendepunkt. \n\n" +
                "Dersom vi finner en oppgave som er flyttet kan vi vurdere om denne oppgaven bør flyttes tilbake.\n\n" +
                "Dersom det finnes en intern ef-oppgave (referanse), vil denne ferdigstilles før vi oppretter en ny oppgave.",
        summary =
            "Lag ny ekstern BehandleSak- eller GodkjenneVedtak-oppgave",
    )
    @PostMapping("gjenopprett-oppgave/behandling/{behandlingId}")
    fun gjenopprettOppgaveForBehandling(
        @PathVariable behandlingId: UUID,
    ) {
        tilgangService.validerHarForvalterrolle()
        val task = GjennoprettOppgavePåBehandlingTask.opprettTask(behandlingId)
        taskService.save(task)
    }

    @PostMapping("ferdigstill")
    fun ferdigstillOppgavtypeForBehandling(
        @RequestBody request: ForvaltningFerdigstillRequest,
    ) {
        tilgangService.validerHarForvalterrolle()
        val task = FerdigstillOppgavetypePåBehandlingTask.opprettTask(request)
        taskService.save(task)
    }
}
