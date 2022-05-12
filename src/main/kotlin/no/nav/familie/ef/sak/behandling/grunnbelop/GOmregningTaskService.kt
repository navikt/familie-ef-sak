package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.beregning.nyesteGrunnbeløpGyldigFraOgMed
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class GOmregningTaskService(val fagsakRepository: FagsakRepository,
                            val taskRepository: TaskRepository) {

    fun opprettGOmregningTaskForBehandlingerMedUtdatertG(): Int {

        val fagsakIder = fagsakRepository.finnFagsakerMedUtdatertGBelop(nyesteGrunnbeløpGyldigFraOgMed)
        val gOmregningTasks = GOmregningTask.opprettTasks(fagsakIder)
        taskRepository.saveAll(gOmregningTasks)
        return gOmregningTasks.size
    }

}