package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.beregning.nyesteGrunnbeløpGyldigFraOgMed
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class GOmregningTaskService(val fagsakRepository: FagsakRepository,
                            val featureToggleService: FeatureToggleService,
                            val gOmregningTask: GOmregningTask) {

    @Scheduled(cron = "0 15 12 23 5 ?")
    fun opprettGOmregningTaskForBehandlingerMedUtdatertG(): Int {

        feilHvisIkke(featureToggleService.isEnabled("familie.ef.sak.omberegning")) {
            "Feature toggle for omberegning er disabled"
        }
        val fagsakIder = fagsakRepository.finnFerdigstilteFagsakerMedUtdatertGBelop(nyesteGrunnbeløpGyldigFraOgMed)
        fagsakIder.forEach {
            gOmregningTask.opprettTask(it)
        }

        return fagsakIder.size
    }

}