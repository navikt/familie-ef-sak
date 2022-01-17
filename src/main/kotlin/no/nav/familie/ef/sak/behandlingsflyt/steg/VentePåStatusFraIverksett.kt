package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandlingsflyt.task.LagSaksbehandlingsblankettTask
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import org.springframework.stereotype.Service

@Service
class VentePåStatusFraIverksett(private val iverksettClient: IverksettClient,
                                private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {

    override fun utførSteg(behandling: Behandling, data: Void?) {
        iverksettClient.hentStatus(behandling.id).let {
            when {
                behandling.erMigrering() && it == IverksettStatus.OK_MOT_OPPDRAG -> opprettLagSaksbehandlingsblankettTask(behandling)
                it == IverksettStatus.OK -> opprettLagSaksbehandlingsblankettTask(behandling)
                else -> throw TaskExceptionUtenStackTrace("Mottok status $it fra iverksett for behandlingId=${behandling.id}")
            }
        }
    }

    fun opprettLagSaksbehandlingsblankettTask(behandling: Behandling) {
        taskRepository.save(LagSaksbehandlingsblankettTask.opprettTask(behandling.id))
    }

    override fun stegType(): StegType {
        return StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
    }

}