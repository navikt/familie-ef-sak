package no.nav.familie.ef.sak.avstemming

import no.nav.familie.ef.sak.behandlingsflyt.task.KonsistensavstemmingPayload
import no.nav.familie.ef.sak.behandlingsflyt.task.KonsistensavstemmingTask
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.prosessering.util.isOptimisticLocking
import org.springframework.context.annotation.Profile
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.random.Random

@Profile("!integrasjonstest")
@Service
class KonsistensavstemmingScheduler(
    private val konsistensavstemmingService: KonsistensavstemmingService,
) {
    private val logger = Logg.getLogger(this::class)

    @Scheduled(cron = "0 0 0/12 * * *")
    fun opprettTasks() {
        Thread.sleep(Random.nextLong(5_000)) // YOLO unngå feil med att 2 noder
        try {
            konsistensavstemmingService.opprettTasks()
        } catch (e: Exception) {
            if (isOptimisticLocking(e)) {
                logger.warn("OptimisticLockingFailureException ved opprettelse av konsistensavstemmingtasks")
            } else {
                logger.error("Feilet opprettelse av tasks for konsistensavstemming", e)
            }
        }
    }
}

@Service
class KonsistensavstemmingService(
    private val repository: KonsistensavstemmingJobbRepository,
    private val taskService: TaskService,
) {
    private val logger = Logg.getLogger(KonsistensavstemmingService::class)

    @Transactional
    fun opprettTasks() {
        val tidspunkt = LocalDate.now().plusDays(2)
        val jobber = repository.findAllByOpprettetIsFalseAndTriggerdatoIsBefore(tidspunkt)
        jobber.forEach {
            val triggerdato = it.triggerdato
            logger.info("Oppretter tasks for konsistensavstemming for dato=$triggerdato")
            taskService.saveAll(
                listOf(
                    KonsistensavstemmingTask.opprettTask(
                        KonsistensavstemmingPayload(StønadType.OVERGANGSSTØNAD, triggerdato),
                        triggerdato.atTime(22, 0),
                    ),
                    KonsistensavstemmingTask.opprettTask(
                        KonsistensavstemmingPayload(StønadType.BARNETILSYN, triggerdato),
                        triggerdato.atTime(22, 20),
                    ),
                ),
            )
        }
        repository.updateAll(jobber.map { it.copy(opprettet = true) })
    }
}

@Repository
interface KonsistensavstemmingJobbRepository :
    RepositoryInterface<KonsistensavstemmingJobb, Int>,
    InsertUpdateRepository<KonsistensavstemmingJobb> {
    fun findAllByOpprettetIsFalseAndTriggerdatoIsBefore(tidspunkt: LocalDate): List<KonsistensavstemmingJobb>
}

data class KonsistensavstemmingJobb(
    @Id
    val id: Int = 0,
    @Version
    val versjon: Int = 0,
    val triggerdato: LocalDate,
    val opprettet: Boolean = false,
)
