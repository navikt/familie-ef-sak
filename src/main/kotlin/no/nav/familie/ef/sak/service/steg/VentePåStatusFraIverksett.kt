package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.task.FerdigstillBehandlingTask
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.prosessering.domene.TaskRepository

class VentePåStatusFraIverksett(private val iverksettClient: IverksettClient, private val taskRepository: TaskRepository): BehandlingSteg<Void?> {

    override fun utførOgReturnerNesteSteg(behandling: Behandling, data: Void?): StegType {
       return iverksettClient.hentStatus(behandling.id).let {
            when (it) {
                IverksettStatus.OK -> {
                    opprettFerdigstillOppgave(behandling)
                    StegType.FERDIGSTILLE_BEHANDLING

                }
                else -> throw error("Mottok status '$it' fra iverksett for behandlingId " + behandling.id)
            }
        }
    }

    fun opprettFerdigstillOppgave(behandling: Behandling) {
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(behandling))
    }

    override fun stegType(): StegType {
        return StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {
        error("Bruker utførOgReturnerNesteSteg")
    }
}