package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.task.VentePåStatusFraØkonomiTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class IverksettMotOppdragSteg(private val tilkjentYtelseService: TilkjentYtelseService,
                              private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {

    override fun validerSteg(behandling: Behandling) {
        // TODO BA-SAK HAR TOTRINNSKONTROLL HER (Se Tea-3218)
    }

    override fun stegType(): StegType {
        return StegType.IVERKSETT_MOT_OPPDRAG
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {
        tilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(behandling)
        venterPåStatusFraØkonomiTask(behandling)
    }

    private fun venterPåStatusFraØkonomiTask(behandling: Behandling) {
        taskRepository.save(VentePåStatusFraØkonomiTask.opprettTask(behandling))
    }
}