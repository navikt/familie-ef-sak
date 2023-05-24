package no.nav.familie.ef.sak.karakterutskrift

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/automatisk-brev-innhenting-karakterutskrift"],
)
@Unprotected
class AutomatiskBrevInnhentingKarakterutskriftController(
    private val automatiskBrevInnhentingKarakterutskriftService: AutomatiskBrevInnhentingKarakterutskriftService,
    private val featureToggleService: FeatureToggleService,
) {

    @PostMapping("/opprett-tasks")
    fun opprettTasks(@RequestBody karakterUtskriftRequest: KarakterutskriftRequest) {
        feilHvis(!featureToggleService.isEnabled(Toggle.AUTOMATISKE_BREV_INNHENTING_KARAKTERUTSKRIFT) && karakterUtskriftRequest.liveRun) {
            "Toggle for automatiske brev for innhenting av karakterutskrift er ikke påskrudd"
        }
        automatiskBrevInnhentingKarakterutskriftService.opprettTasks(KarakterutskriftBrevtype.HOVEDPERIODE, liveRun = karakterUtskriftRequest.liveRun)
    }
}

data class KarakterutskriftRequest(val liveRun: Boolean)
