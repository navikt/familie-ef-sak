package no.nav.familie.ef.sak.minside

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = AktiverMikrofrontendTask.TYPE,
    beskrivelse = "Sender over person til kafka-kø for aktivering av minside-mikrofrontend",
)
class AktiverMikrofrontendTask(
    val minSideKafkaProducerService: MinSideKafkaProducerService,
    val fagsakPersonService: FagsakPersonService,
) : AsyncTaskStep {
    private val logger = Logg.getLogger(this::class)

    override fun doTask(task: Task) {
        logger.info("Starter task for aktivering av bruker for mikrofrontend")
        val (fagsakId, fagsakPersonId) = objectMapper.readValue<AktiverMikrofrontendDto>(task.payload)
        val fagsakPerson =
            utledFagsakPerson(fagsakPersonId, fagsakId)
        if (fagsakPerson.harAktivertMikrofrontend) {
            logger.info("Fagsakperson med id=${fagsakPerson.id} har allerede aktivert mikrofrontend")
        } else {
            minSideKafkaProducerService.aktiver(fagsakPerson.hentAktivIdent())
            fagsakPersonService.oppdaterMedMikrofrontendAktivering(fagsakPerson.id, true)
        }
    }

    private fun utledFagsakPerson(
        fagsakPersonId: UUID?,
        fagsakId: UUID?,
    ): FagsakPerson =
        when {
            fagsakPersonId != null -> fagsakPersonService.hentPerson(fagsakPersonId)
            fagsakId != null -> fagsakPersonService.finnFagsakPersonForFagsakId(fagsakId)
            else -> error("Mangler både fagsakId og fagsakPersonId - kan ikke aktivere mikrofrontend for bruker")
        }

    companion object {
        fun opprettTask(fagsakPerson: FagsakPerson): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(AktiverMikrofrontendDto(fagsakPersonId = fagsakPerson.id)),
            )

        fun opprettTaskMedFagsakId(fagsakId: UUID): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(AktiverMikrofrontendDto(fagsakId = fagsakId)),
            )

        const val TYPE = "aktiverMikrofrontendTask"
    }

    data class AktiverMikrofrontendDto(
        val fagsakId: UUID? = null,
        val fagsakPersonId: UUID? = null,
        val opprettetTid: LocalDateTime = LocalDateTime.now(),
    ) {
        init {
            require(fagsakId != null || fagsakPersonId != null) { "Må sette enten fagsakId eller fagsakPersonId" }
        }
    }
}
