package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.task.LagSaksbehandlingsblankettTask
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import org.springframework.stereotype.Service

@Service
class VentePåStatusFraIverksett(
    private val iverksettClient: IverksettClient,
    private val taskRepository: TaskRepository
) : BehandlingSteg<Void?> {

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        iverksettClient.hentStatus(saksbehandling.id).let {
            when {
                erBrevløsIverksettingOk(saksbehandling, it) -> opprettLagSaksbehandlingsblankettTask(saksbehandling)
                setOf(IverksettStatus.JOURNALFØRT, it == IverksettStatus.OK).contains(it) -> opprettLagSaksbehandlingsblankettTask(saksbehandling)
                else -> throw TaskExceptionUtenStackTrace("Mottok status $it fra iverksett for behandlingId=${saksbehandling.id}")
            }
        }
    }

    private fun erBrevløsIverksettingOk(
        saksbehandling: Saksbehandling,
        status: IverksettStatus
    ): Boolean {
        if (saksbehandling.årsak !in setOf(
                BehandlingÅrsak.KORRIGERING_UTEN_BREV,
                BehandlingÅrsak.G_OMREGNING,
                BehandlingÅrsak.MIGRERING
            )
        ) {
            return false
        }
        return status == IverksettStatus.OK_MOT_OPPDRAG
    }

    fun opprettLagSaksbehandlingsblankettTask(saksbehandling: Saksbehandling) {
        taskRepository.save(LagSaksbehandlingsblankettTask.opprettTask(saksbehandling.id))
    }

    override fun stegType(): StegType {
        return StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
    }
}
