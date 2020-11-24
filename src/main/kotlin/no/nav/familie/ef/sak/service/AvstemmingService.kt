package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.avstemming.AvstemmingDto
import no.nav.familie.ef.sak.api.avstemming.AvstemmingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.task.KonsistensavstemmingTask
import no.nav.familie.ef.sak.task.GrensesnittavstemmingTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.util.VirkedagerProvider
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class AvstemmingService(val grensesnittavstemmingTask: GrensesnittavstemmingTask,
                        val konsistensavstemmingTask: KonsistensavstemmingTask) {

    fun utløsAvstemming(avstemmingDto: AvstemmingDto): Task? = with(avstemmingDto) {
        when (avstemmingType) {
            AvstemmingType.GRENSESNITTAVSTEMMING -> utløsGrensesnittavstemming(fraDato = fraDato,
                                                                                stønadstype = stønadstype,
                                                                                triggerTid = triggerTid)
            AvstemmingType.KONSISTENSAVSTEMMING -> utløsKonsistensAvstemming(stønadstype, triggerTid)
        }
    }

    fun utløsGrensesnittavstemming(fraDato: LocalDate, stønadstype: Stønadstype, triggerTid: LocalDateTime?): Task {
        val nesteVirkedag: LocalDateTime = triggerTid ?: VirkedagerProvider.nesteVirkedag(fraDato).atTime(8, 0)
        return grensesnittavstemmingTask.opprettNyTask(fraDato, nesteVirkedag, stønadstype)
    }

    fun utløsKonsistensAvstemming(stønadstype: Stønadstype, triggerTid: LocalDateTime = LocalDateTime.now()): Task {
        return konsistensavstemmingTask.opprettNyTask(triggerTid, stønadstype)
    }

}
