package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.LoggOppgaveMetadataTask
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
    private val featureToggleService: FeatureToggleService,
) {
    @PostMapping("behandling/{behandlingId}")
    fun loggOppgavemetadataFor(@PathVariable behandlingId: UUID) {
        feilHvisIkke(erUtviklerMedVeilderrolle()) { "Kan kun kjøres av utvikler med veilederrolle" }
        val task = LoggOppgaveMetadataTask.opprettTask(behandlingId)
        taskService.save(task)
    }

    private fun erUtviklerMedVeilderrolle(): Boolean =
        SikkerhetContext.erSaksbehandler() && featureToggleService.isEnabled(Toggle.UTVIKLER_MED_VEILEDERRROLLE)
}
