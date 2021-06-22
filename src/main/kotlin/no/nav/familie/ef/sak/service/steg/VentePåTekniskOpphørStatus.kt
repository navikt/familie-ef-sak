package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.task.FerdigstillBehandlingTask
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class VentePåTekniskOpphørStatus(private val iverksettClient: IverksettClient,
                                 private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {

    override fun utførSteg(behandling: Behandling, data: Void?) {
        return iverksettClient.hentStatus(behandling.id).let {
            when (it) {
                IverksettStatus.OK_MOT_OPPDRAG -> opprettLagSaksbehandlingsblankettTask(behandling)
                else -> throw error("Mottok status $it fra iverksett for behandlingId=${behandling.id}")
            }
        }
    }

    fun opprettLagSaksbehandlingsblankettTask(behandling: Behandling) {
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(behandling))
    }

    override fun stegType(): StegType {
        return StegType.VENTE_PÅ_TEKNISK_OPPHØR_STATUS
    }
}