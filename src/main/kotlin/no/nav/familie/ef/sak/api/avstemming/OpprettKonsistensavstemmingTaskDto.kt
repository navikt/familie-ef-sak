package no.nav.familie.ef.sak.api.avstemming

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.task.KonsistensavstemmingPayload
import no.nav.familie.ef.sak.task.KonsistensavstemmingTask
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import java.time.LocalDate

data class OpprettKonsistensavstemmingTaskDto(val datoForAvstemming: LocalDate, val stønadstype: Stønadstype)


fun OpprettKonsistensavstemmingTaskDto.tilTask(): Task {
    val triggerTid = this.datoForAvstemming.atTime(8, 0)
    val payload = objectMapper.writeValueAsString(KonsistensavstemmingPayload(stønadstype = this.stønadstype,
                                                                                  triggerTid = triggerTid))
    return Task(type = KonsistensavstemmingTask.TYPE, payload = payload, triggerTid = triggerTid)
}