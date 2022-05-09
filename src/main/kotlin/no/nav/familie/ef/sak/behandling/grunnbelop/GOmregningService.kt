package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.beregning.finnGrunnbeløp
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class GOmregningService(val behandlingRepository: BehandlingRepository,
                        val taskRepository: TaskRepository) {

    fun opprettGOmregningTaskForBehandlingerMedUtdatertG(): Int {

        val nyesteGrunnbeløp = finnGrunnbeløp(LocalDate.now()).fraOgMedDato
        val behandlingIds = behandlingRepository.finnBehandlingerMedUtdatertGBelop(nyesteGrunnbeløp)
        val gOmregningTasks = GOmregningTask.opprettTasks(behandlingIds)
        taskRepository.saveAll(gOmregningTasks)
        return gOmregningTasks.size
    }

    fun oppdaterBehandlingMedNyG(behandlingId: UUID) {
        TODO("Not yet implemented")
    }
}