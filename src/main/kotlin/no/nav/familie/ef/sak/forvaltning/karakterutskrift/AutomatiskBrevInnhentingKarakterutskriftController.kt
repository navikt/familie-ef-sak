package no.nav.familie.ef.sak.forvaltning.karakterutskrift

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/automatisk-brev-innhenting-karakterutskrift"],
)
@ProtectedWithClaims(issuer = "azuread")
class AutomatiskBrevInnhentingKarakterutskriftController(
    private val automatiskBrevInnhentingKarakterutskriftService: AutomatiskBrevInnhentingKarakterutskriftService,
    private val featureToggleService: FeatureToggleService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("/opprett-tasks")
    fun opprettTasks(
        @RequestBody karakterUtskriftRequest: KarakterutskriftRequest,
    ) {
        tilgangService.validerHarForvalterrolle()
        feilHvis(!featureToggleService.isEnabled(Toggle.AUTOMATISKE_BREV_INNHENTING_KARAKTERUTSKRIFT) && karakterUtskriftRequest.liveRun) {
            "Toggle for automatiske brev for innhenting av karakterutskrift er ikke påskrudd"
        }
        validerBrevtype(karakterUtskriftRequest.frittståendeBrevType)

        automatiskBrevInnhentingKarakterutskriftService.opprettTasks(
            brevtype = karakterUtskriftRequest.frittståendeBrevType,
            liveRun = karakterUtskriftRequest.liveRun,
            taskLimit = karakterUtskriftRequest.taskLimit,
        )
    }

    private fun validerBrevtype(brevtype: FrittståendeBrevType) {
        feilHvis(
            brevtype != FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE &&
                brevtype != FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_UTVIDET_PERIODE,
        ) {
            "Skal ikke opprette automatiske innhentingsbrev for frittstående brev av type $brevtype"
        }
    }
}

data class KarakterutskriftRequest(val liveRun: Boolean, val frittståendeBrevType: FrittståendeBrevType, val taskLimit: Int)
