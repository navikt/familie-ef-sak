package no.nav.familie.ef.sak.karakterutskrift

import no.nav.familie.ef.sak.felles.util.EnvUtil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.PathVariable
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
        validerBrevtype(karakterUtskriftRequest.frittståendeBrevType)

        automatiskBrevInnhentingKarakterutskriftService.opprettTasks(
            brevtype = karakterUtskriftRequest.frittståendeBrevType,
            liveRun = karakterUtskriftRequest.liveRun,
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

    @PostMapping("/test/opprett-task/oppgave/{oppgaveId}")
    fun opprettTaskForOppgave(@PathVariable oppgaveId: Long): Ressurs<Unit> {
        feilHvisIkke(EnvUtil.erIDev()) {
            "Kan ikke opprette KarakterutskriftbrevTask hvis miljø ikke er dev"
        }
        return Ressurs.success(automatiskBrevInnhentingKarakterutskriftService.opprettTaskForOppgave(oppgaveId))
    }
}

data class KarakterutskriftRequest(val liveRun: Boolean, val frittståendeBrevType: FrittståendeBrevType)
