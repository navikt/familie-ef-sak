package no.nav.familie.ef.sak.minside

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = MikrofrontendEnableBrukereTask.TYPE,
    beskrivelse = "Sender over liste av personer til minside kafka-kø",
)
class MikrofrontendEnableBrukereTask(val minSideKafkaProducerService: MinSideKafkaProducerService): AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        logger.info("Starter task for aktivering av brukere for mikrofrontend")
        val fagsakPersonIdenter = objectMapper.readValue<List<FagsakPerson>>(task.payload)
        fagsakPersonIdenter.forEach { fagsakPerson ->
            minSideKafkaProducerService.aktiver(fagsakPerson.hentAktivIdent())
        }
    }

    companion object {

        fun opprettTask(fagsakPerson: List<FagsakPerson>): Task {
            return Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(fagsakPerson),
            )
        }
        const val TYPE = "mikrofrontendEnableBrukereTask"
    }
}