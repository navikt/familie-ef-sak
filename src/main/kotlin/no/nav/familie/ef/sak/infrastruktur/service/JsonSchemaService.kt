package no.nav.familie.ef.sak.infrastruktur.service

import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/jsonUpdate"])
@Unprotected
class JsonSchemaService(
    private val taskService: TaskService,
    private val vedtakRepository: VedtakRepository
) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    @GetMapping
    fun update(): String {
        val vedtakIder = vedtakRepository.finnAlleIder()

        log.info("Starter oppretting av tasker for oppdatering av json på vedtak. Antall ${vedtakIder.size}")
        vedtakIder.forEach {
            taskService.save(Task(JsonUpdateVedtakTask.TYPE, it.toString()))
        }
        log.info("oppretting av ${vedtakIder.size} tasker fullført")

        return "Opprettet ${vedtakIder.size} tasker for oppdatering av vedtak. "
    }
}

@Service
@TaskStepBeskrivelse(
    taskStepType = JsonUpdateVedtakTask.TYPE,
    maxAntallFeil = 5,
    triggerTidVedFeilISekunder = 15,
    beskrivelse = "Oppdaterer Json-data."
)
class JsonUpdateVedtakTask(
    private val vedtakRepository: VedtakRepository,
) : AsyncTaskStep {
    override fun doTask(task: Task) {

        val vedtak = vedtakRepository.findByIdOrThrow(UUID.fromString(task.payload))
        vedtakRepository.update(vedtak)
    }

    companion object {
        const val TYPE = "JsonUpdateVedtak"
    }
}
