package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.beregning.nyesteGrunnbeløpGyldigFraOgMed
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class GOmregningTaskService(val fagsakRepository: FagsakRepository,
                            val featureToggleService: FeatureToggleService,
                            val taskRepository: TaskRepository) {

    fun opprettGOmregningTaskForBehandlingerMedUtdatertG(): Int {

        feilHvisIkke(featureToggleService.isEnabled("familie.ef.sak.omberegning")) {
            "Feature toggle for omberegning er disabled"
        }
        val fagsakIder = fagsakRepository.finnFerdigstilteFagsakerMedUtdatertGBelop(nyesteGrunnbeløpGyldigFraOgMed)
        val gOmregningTasks = GOmregningTask.opprettTasks(fagsakIder)
        taskRepository.saveAll(gOmregningTasks)
        return gOmregningTasks.size
    }

}