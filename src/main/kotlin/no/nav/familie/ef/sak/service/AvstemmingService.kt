package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.avstemming.GrensesnittavstemmingDto
import no.nav.familie.ef.sak.api.avstemming.OpprettKonsistensavstemmingTaskDto
import no.nav.familie.ef.sak.api.avstemming.tilTask
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.vedtaksbrev.IverksettClient
import no.nav.familie.ef.sak.økonomi.tilKlassifisering
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingDto
import no.nav.familie.kontrakter.felles.oppdrag.GrensesnittavstemmingRequest
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class AvstemmingService(private val oppdragClient: OppdragClient,
                        private val iverksettClient: IverksettClient,
                        private val taskRepository: TaskRepository,
                        private val tilkjentYtelseService: TilkjentYtelseService) {

    fun opprettGrensesnittavstemmingTask(grensesnittavstemmingDto: GrensesnittavstemmingDto) =
            grensesnittavstemmingDto
                    .let { it.tilTask() }
                    .let { taskRepository.save(it) }

    fun opprettKonsistenavstemmingTasker(avstemmingTaskDtoOpprett: List<OpprettKonsistensavstemmingTaskDto>) =
            avstemmingTaskDtoOpprett
                    .map { it.tilTask() }
                    .let { taskRepository.saveAll(it) }
                    .let { it.toList() }

    fun grensesnittavstemOppdrag(fraTidspunkt: LocalDateTime, tilTidspunkt: LocalDateTime, stønadstype: Stønadstype) {
        val grensesnittavstemmingRequest = GrensesnittavstemmingRequest(fagsystem = stønadstype.tilKlassifisering(),
                                                                        fra = fraTidspunkt,
                                                                        til = tilTidspunkt)
        oppdragClient.grensesnittavstemming(grensesnittavstemmingRequest)
    }

    fun konsistensavstemOppdrag(stønadstype: Stønadstype) {
        val datoForAvstemming = LocalDate.now()
        val tilkjenteYtelser = tilkjentYtelseService
                .finnTilkjentYtelserTilKonsistensavstemming(datoForAvstemming = datoForAvstemming, stønadstype = stønadstype)
        iverksettClient.konsistensavstemming(KonsistensavstemmingDto(StønadType.valueOf(stønadstype.name), tilkjenteYtelser))
    }

}
