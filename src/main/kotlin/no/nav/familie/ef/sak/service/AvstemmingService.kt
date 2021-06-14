package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.avstemming.OpprettKonsistensavstemmingTaskDto
import no.nav.familie.ef.sak.api.avstemming.tilTask
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingDto
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AvstemmingService(private val iverksettClient: IverksettClient,
                        private val taskRepository: TaskRepository,
                        private val tilkjentYtelseService: TilkjentYtelseService) {

    fun opprettKonsistenavstemmingTasker(avstemmingDto: List<OpprettKonsistensavstemmingTaskDto>) =
            avstemmingDto
                    .map { it.tilTask() }
                    .let { taskRepository.saveAll(it) }
                    .let { it.toList() }

    fun konsistensavstemOppdrag(stønadstype: Stønadstype, datoForAvstemming: LocalDate) {
        val tilkjenteYtelser = tilkjentYtelseService
                .finnTilkjentYtelserTilKonsistensavstemming(datoForAvstemming = datoForAvstemming, stønadstype = stønadstype)
        iverksettClient.konsistensavstemming(KonsistensavstemmingDto(StønadType.valueOf(stønadstype.name), tilkjenteYtelser))
    }

}
