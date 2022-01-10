package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillBehandlingTask
import no.nav.familie.ef.sak.behandlingsflyt.task.LagSaksbehandlingsblankettTask
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import org.springframework.stereotype.Service

@Service
class VentePåStatusFraIverksett(private val iverksettClient: IverksettClient,
                                private val behandlingService: BehandlingService,
                                private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {

    override fun utførSteg(behandling: Behandling, data: Void?) {
        return iverksettClient.hentStatus(behandling.id).let {
            val erOk = it == IverksettStatus.OK
            when {
                behandling.erMigrering() && erOk -> gåDirekteTilFerdigstillBehandlingTask(behandling)
                erOk -> opprettLagSaksbehandlingsblankettTask(behandling)
                else -> throw TaskExceptionUtenStackTrace("Mottok status $it fra iverksett for behandlingId=${behandling.id}")
            }
        }
    }

    private fun gåDirekteTilFerdigstillBehandlingTask(behandling: Behandling) {
        val oppdatertBehandling = behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.FERDIGSTILLE_BEHANDLING)
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(oppdatertBehandling))
    }

    fun opprettLagSaksbehandlingsblankettTask(behandling: Behandling) {
        taskRepository.save(LagSaksbehandlingsblankettTask.opprettTask(behandling.id))
    }

    override fun stegType(): StegType {
        return StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
    }

}