package no.nav.familie.ef.sak.minside

import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = DeaktiverMikrofrontendTask.TYPE,
    beskrivelse = "Sender over person til kafka-kø for deaktivering av minside-mikfrofrontend",
)
class DeaktiverMikrofrontendTask(
    val minSideKafkaProducerService: MinSideKafkaProducerService,
    val fagsakPersonService: FagsakPersonService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        logger.info("Starter task for deaktivering av bruker for mikrofrontend")
        val (fagsakPersonId) = jsonMapper.readValue<DeaktiverMikrofrontendDto>(task.payload)
        val fagsakPerson = fagsakPersonService.hentPerson(fagsakPersonId)
        if (fagsakPerson.harAktivertMikrofrontend) {
            minSideKafkaProducerService.deaktiver(fagsakPerson.hentAktivIdent())
            fagsakPersonService.oppdaterMedMikrofrontendAktivering(fagsakPersonId, false)
        } else {
            logger.info("Fagsakperson med id=$fagsakPersonId har allerede aktivert mikrofrontend")
        }
    }

    companion object {
        fun opprettTask(fagsakPersonId: UUID): Task =
            Task(
                type = TYPE,
                payload = jsonMapper.writeValueAsString(DeaktiverMikrofrontendDto(fagsakPersonId)),
                properties =
                    Properties().apply {
                        this["fagsakPersonId"] = fagsakPersonId.toString()
                    },
            )

        const val TYPE = "deaktiverMikrofrontendTask"
    }

    data class DeaktiverMikrofrontendDto(
        val fagsakPersonId: UUID,
        val opprettetTid: LocalDateTime = LocalDateTime.now(),
    )
}
