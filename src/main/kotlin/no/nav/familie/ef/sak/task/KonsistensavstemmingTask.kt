package no.nav.familie.ef.sak.task

import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.repository.KonsistensavstemmingRepository
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.økonomi.tilKlassifisering
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.KonsistensavstemmingRequest
import no.nav.familie.kontrakter.felles.oppdrag.OppdragIdForFagsystem
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import com.fasterxml.jackson.module.kotlin.readValue

@Service
@TaskStepBeskrivelse(taskStepType = KonsistensavstemmingTask.TYPE, beskrivelse = "Utfører konsistensavstemming mot økonomi.")
class KonsistensavstemmingTask(
        private val oppdragClient: OppdragClient,
        private val taskRepository: TaskRepository,
        private val konsistensavstemmingRepository: KonsistensavstemmingRepository,
) : AsyncTaskStep {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue<KonsistensavstemmingPayload>(task.payload)
        konsistensavstemmingRepository.finnKonsistensavstemmingMedDatoIdag(stønadstype = payload.stønadstype)
                ?.let { utførKonsistensavstemming(payload.stønadstype) }

    }

    override fun onCompletion(task: Task) {
        val payload = objectMapper.readValue<KonsistensavstemmingPayload>(task.payload)
        opprettNyTask(triggerTid = LocalDate.now().plusDays(1).atTime(8, 0), stønadstype = payload.stønadstype)

    }

    fun opprettNyTask(triggerTid: LocalDateTime, stønadstype: Stønadstype): Task {
        val nesteAvstemmingTask = Task(type = TYPE,
                                       payload = objectMapper.writeValueAsString(KonsistensavstemmingPayload(stønadstype, triggerTid = triggerTid)),
                                       triggerTid = triggerTid)
        return taskRepository.save(nesteAvstemmingTask)
    }


    private fun utførKonsistensavstemming(stønadstype: Stønadstype) {
        val oppdragIdListe = emptyList<OppdragIdForFagsystem>() //TODO HENT behandlingerne som skall avstemmes
        oppdragClient.konsistensavstemming(KonsistensavstemmingRequest(
                fagsystem = stønadstype.tilKlassifisering(),
                oppdragIdListe = oppdragIdListe,
                avstemmingstidspunkt = LocalDate.now().atStartOfDay() //TODO Vad skal in her her?
        ))
    }

    companion object {

        const val TYPE = "utførKonsistensavstemming"
    }


}