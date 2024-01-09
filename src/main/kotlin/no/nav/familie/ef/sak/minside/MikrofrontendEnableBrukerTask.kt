package no.nav.familie.ef.sak.minside

import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = MikrofrontendEnableBrukerTask.TYPE,
    beskrivelse = "Sender over person til kafka-kø for minside",
)
class MikrofrontendEnableBrukerTask(val minSideKafkaProducerService: MinSideKafkaProducerService) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        logger.info("Starter task for aktivering av bruker for mikrofrontend")
        val personIdent = task.payload
        minSideKafkaProducerService.aktiver(personIdent)
    }

    companion object {

        fun opprettTask(fagsakPerson: FagsakPerson): Task {
            return Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(listOf(fagsakPerson.hentAktivIdent())),
            )
        }
        const val TYPE = "mikrofrontendEnableBrukerTask"
    }
}
