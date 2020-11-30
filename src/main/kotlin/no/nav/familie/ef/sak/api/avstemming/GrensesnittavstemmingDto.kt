package no.nav.familie.ef.sak.api.avstemming

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.task.GrensesnittavstemmingPayload
import no.nav.familie.ef.sak.task.GrensesnittavstemmingTask
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.util.VirkedagerProvider
import java.time.LocalDate
import java.time.LocalDateTime

data class GrensesnittavstemmingDto(val stønadstype: Stønadstype, val fraDato: LocalDate, val triggerTid: LocalDateTime? = null)

fun GrensesnittavstemmingDto.tilTask(): Task {
    val nesteVirkedag: LocalDateTime = triggerTid ?: VirkedagerProvider.nesteVirkedag(fraDato).atTime(8, 0)
    val payload =
            objectMapper.writeValueAsString(GrensesnittavstemmingPayload(fraDato = this.fraDato,
                                                                         stønadstype = this.stønadstype))

    return Task(type = GrensesnittavstemmingTask.TYPE,
                payload = payload,
                triggerTid = nesteVirkedag)
}