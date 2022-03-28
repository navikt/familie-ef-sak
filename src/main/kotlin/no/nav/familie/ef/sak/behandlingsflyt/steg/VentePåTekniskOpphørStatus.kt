package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillBehandlingTask
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class VentePåTekniskOpphørStatus(private val iverksettClient: IverksettClient,
                                 private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        return iverksettClient.hentStatus(saksbehandling.id).let {
            when (it) {
                IverksettStatus.OK_MOT_OPPDRAG -> opprettFerdigstillOppgave(saksbehandling)
                else -> error("Mottok status $it fra iverksett for behandlingId=${saksbehandling.id}")
            }
        }
    }

    fun opprettFerdigstillOppgave(saksbehandling: Saksbehandling) {
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(saksbehandling))
    }

    override fun stegType(): StegType {
        return StegType.VENTE_PÅ_TEKNISK_OPPHØR_STATUS
    }
}