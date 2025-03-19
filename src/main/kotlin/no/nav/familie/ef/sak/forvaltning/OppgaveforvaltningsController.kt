package no.nav.familie.ef.sak.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ef.sak.behandling.revurdering.RevurderingService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

enum class OppgavetypeSomKanFerdigstilles {
    BehandleSak,
    GodkjenneVedtak,
    ;

    fun tilOppgavetype(): Oppgavetype =
        when (this) {
            BehandleSak -> Oppgavetype.BehandleSak
            GodkjenneVedtak -> Oppgavetype.GodkjenneVedtak
        }
}

data class ForvaltningFerdigstillRequest(
    val behandlingId: UUID,
    val oppgavetype: OppgavetypeSomKanFerdigstilles,
    val opprettetTid: LocalDateTime = LocalDateTime.now(),
)

@RestController
@RequestMapping("/api/oppgave/forvaltning/")
@ProtectedWithClaims(issuer = "azuread")
class OppgaveforvaltningsController(
    private val taskService: TaskService,
    private val tilgangService: TilgangService,
    private val personHendelseClient: PersonHendelseClient,
    private val revurderingService: RevurderingService,
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

    @Operation(
        description =
            "Hvis en behandling har flere oppgaver av samme type som ikke er ferdigstilt kan vi bruke dette endepunktet for å ferdigstille en av oppgavene.",
        summary =
            "Ferdigstill BehandleSak- eller GodkjenneVedtak-oppgave i EF-sak og feilregistrer  oppgave i Gosys",
    )
    @PostMapping("ferdigstill/oppgavetype/{oppgavetype}/behandlingid/{behandlingId}")
    fun ferdigstillOppgavtypeForBehandling(
        @PathVariable behandlingId: UUID,
        @PathVariable oppgavetype: OppgavetypeSomKanFerdigstilles,
    ) {
        tilgangService.validerHarForvalterrolle()
        val task = FerdigstillOppgavetypePåBehandlingTask.opprettTask(ForvaltningFerdigstillRequest(behandlingId = behandlingId, oppgavetype = oppgavetype))
        taskService.save(task)
    }

    // TODO: Denne skal fjernes, kun for testing gjennom Swagger.
    @Operation(
        description = "Test kall som får ef-personhendelse til å begynne å lage BehandleAutomatiskInntektsendringTasker for identer i pre-prod.",
        summary = "Personhendelse kaller på EF-sak, henter identer, sjekker identer og returener identer for generering av BehandleAutomatiskInntektsendringTasker i ef-sak.",
    )
    @PostMapping("/manuellOpprettelseAvBehandleAutomatiskInntektsendringTasker")
    fun manuellOpprettelseAvBehandleAutomatiskInntektsendringTasker() {
        personHendelseClient.opprettBehandleAutomatiskInntektsendringTasker()
    }

    // TODO: Denne skal fjernes, kun for testing gjennom Swagger.
    @Operation(
        description = "Test kall som sender med en ident, der hensikten er å teste om personhendelse klarer å lage en BehandleAutomatiskInntektsendringTask for en gitt personIdent.",
        summary = "Tar inn en personIdent og får personHendelse til å manuelt opprette en BehandleAutomatiskInntektsendringTask for gitt personIdent.",
    )
    @PostMapping("/manuellOpprettelseAvBehandleAutomatiskInntektsendringTask")
    fun manuellOpprettelseAvBehandleAutomatiskInntektsendringTask(
        @RequestBody manuellOpprettelseAvBehandleAutomatiskInntektsendringTaskRequestBody: ManuellOpprettelseAvBehandleAutomatiskInntektsendringTaskRequestBody,
    ) {
        revurderingService.opprettAutomatiskInntektsendringTask(personident = manuellOpprettelseAvBehandleAutomatiskInntektsendringTaskRequestBody.personIdent)
    }
}

// TODO: Denne skal fjernes, kun for testing gjennom Swagger.
data class ManuellOpprettelseAvBehandleAutomatiskInntektsendringTaskRequestBody(
    val personIdent: String,
)
