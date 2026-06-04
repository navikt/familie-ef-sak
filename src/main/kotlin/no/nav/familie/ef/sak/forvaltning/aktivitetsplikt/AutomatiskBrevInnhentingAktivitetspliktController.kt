package no.nav.familie.ef.sak.forvaltning.aktivitetsplikt

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/automatisk-brev-innhenting-aktivitetsplikt"],
)
@ProtectedWithClaims(issuer = "azuread")
class AutomatiskBrevInnhentingAktivitetspliktController(
    private val taskService: TaskService,
    private val featureToggleService: FeatureToggleService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("/opprett-tasks")
    fun opprettTasks(
        @RequestBody aktivitetspliktRequest: AktivitetspliktRequest,
    ) {
        tilgangService.validerHarForvalterrolle()
        feilHvis(!featureToggleService.isEnabled(Toggle.AUTOMATISKE_BREV_INNHENTING_AKTIVITETSPLIKT) && aktivitetspliktRequest.liveRun) {
            "Toggle for automatiske brev for innhenting av aktiitetsplikt er ikke påskrudd"
        }

        taskService.save(StartUtsendingAvAktivitetspliktBrevTask.opprettTask(aktivitetspliktRequest))
    }
}

data class AktivitetspliktRequest(
    val liveRun: Boolean,
    val taskLimit: Int,
)
