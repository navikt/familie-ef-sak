package no.nav.familie.ef.sak.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.OppdragIdForFagsystem
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = KonsistensavstemmingTask.TYPE, beskrivelse = "Utfører konsistensavstemming mot økonomi.")
class KonsistensavstemmingTask(
        private val avstemmingService: AvstemmingService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue<KonsistensavstemmingPayload>(task.payload)
        utførKonsistensavstemming(payload.stønadstype)
    }

    private fun utførKonsistensavstemming(stønadstype: Stønadstype) {
        val oppdragIdListe = emptyList<OppdragIdForFagsystem>() //TODO HENT behandlingerne som skall avstemmes
        avstemmingService.konsistensavstemOppdrag(stønadstype, oppdragIdListe)
    }

    companion object {

        const val TYPE = "utførKonsistensavstemming"
    }


}