package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.blankett.BlankettService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.task.FerdigstillBehandlingTask
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class SaksbehandlingsblankettSteg(private val blankettService: BlankettService,
                                  private val taskRepository: TaskRepository) :
        BehandlingSteg<Void?> {

    override fun utførSteg(behandling: Behandling, data: Void?) {
        blankettService.lagBlankett(behandling.id)
        opprettFerdigstillOppgave(behandling)
    }

    override fun stegType(): StegType {
        return StegType.LAG_SAKSBEHANDLINGSBLANKETT
    }

    fun opprettFerdigstillOppgave(behandling: Behandling) {
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(behandling))
    }

}