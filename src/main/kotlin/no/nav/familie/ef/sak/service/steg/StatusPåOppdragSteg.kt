package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime


@Service
class StatusPåOppdragSteg(private val taskRepository: TaskRepository,
                          private val tilkjentYtelseService: TilkjentYtelseService) : BehandlingSteg<Task> {

    override fun utførSteg(behandling: Behandling, data: Task) {
        tilkjentYtelseService.hentStatus(behandling).let {
            when (it) {
                OppdragStatus.LAGT_PÅ_KØ ->
                    taskRepository.save(data.copy(triggerTid = LocalDateTime.now().plusMinutes(15)))
                OppdragStatus.KVITTERT_OK -> TODO()
                else -> {
                    taskRepository.save(data.copy(status = Status.MANUELL_OPPFØLGING))
                    error("Mottok status '$it' fra oppdrag")
                }
            }
        }
    }


    override fun stegType(): StegType {
        return StegType.STATUS_PÅ_OPPDRAG
    }
}