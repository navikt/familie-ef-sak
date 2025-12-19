package no.nav.familie.ef.sak.minside

import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = AktiverMikrofrontendNyttFødselsnummerTask.TYPE,
    beskrivelse = "Sender over person med nytt fødselsnummer til kafka-kø for aktivering av minside-mikrofrontend",
)
class AktiverMikrofrontendNyttFødselsnummerTask(
    val minSideKafkaProducerService: MinSideKafkaProducerService,
    val fagsakPersonService: FagsakPersonService,
) : AsyncTaskStep {
    private val logger = Logg.getLogger(this::class)

    override fun doTask(task: Task) {
        logger.info("Starter task for aktivering av bruker med nytt fødselsnummer for mikrofrontend")
        val fagsakPersonId = UUID.fromString(task.payload)
        val fagsakPerson = fagsakPersonService.hentPerson(fagsakPersonId)
        minSideKafkaProducerService.aktiver(fagsakPerson.hentAktivIdent())
        fagsakPersonService.oppdaterMedMikrofrontendAktivering(fagsakPersonId, true)
    }

    companion object {
        fun opprettTask(fagsakPerson: FagsakPerson): Task = Task(type = TYPE, payload = fagsakPerson.id.toString())

        const val TYPE = "aktiverMikrofrontendForNyttFodselsnummerTask"
    }
}
