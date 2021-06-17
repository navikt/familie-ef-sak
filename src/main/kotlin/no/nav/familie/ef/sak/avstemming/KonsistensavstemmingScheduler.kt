package no.nav.familie.ef.sak.avstemming

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.task.KonsistensavstemmingPayload
import no.nav.familie.ef.sak.task.KonsistensavstemmingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.random.Random

@Service
class KonsistensavstemmingScheduler(private val repository: KonsistensavstemmingJobbRepository,
                                    private val taskRepository: TaskRepository) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 0/12 * * *")
    @Transactional
    fun opprettTasks() {
        Thread.sleep(Random.nextLong(20)) // YOLO unngå feil med att 2 noder
        val tidspunkt = LocalDate.now().plusDays(2)
        val jobber = repository.findAllByOpprettetIsFalseAndTriggerdatoIsBefore(tidspunkt)
        jobber.forEach {
            val triggerTid = it.triggerdato.atTime(8, 0)
            logger.info("Oppretter task for triggerTid=$triggerTid")
            val payload = KonsistensavstemmingPayload(Stønadstype.OVERGANGSSTØNAD, triggerTid)
            taskRepository.save(KonsistensavstemmingTask.opprettTask(payload))
        }
        repository.updateAll(jobber.map { it.copy(opprettet = true) })
    }

}

@Repository
interface KonsistensavstemmingJobbRepository : RepositoryInterface<KonsistensavstemmingJobb, Int>,
                                               InsertUpdateRepository<KonsistensavstemmingJobb> {

    fun findAllByOpprettetIsFalseAndTriggerdatoIsBefore(tidspunkt: LocalDate): List<KonsistensavstemmingJobb>
}

data class KonsistensavstemmingJobb(@Id
                                    val id: Int = 0,
                                    @Version
                                    val versjon: Int = 0,
                                    val triggerdato: LocalDate,
                                    val opprettet: Boolean = false)