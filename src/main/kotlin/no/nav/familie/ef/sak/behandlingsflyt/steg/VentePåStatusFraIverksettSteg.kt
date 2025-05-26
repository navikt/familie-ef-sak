package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.task.LagSaksbehandlingsblankettTask
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service

@Service
class VentePåStatusFraIverksettSteg(
    private val iverksettClient: IverksettClient,
    private val taskService: TaskService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        iverksettClient.hentStatus(saksbehandling.id).let {
            when {
                saksbehandling.skalIkkeSendeBrev && it == IverksettStatus.OK_MOT_OPPDRAG -> opprettLagSaksbehandlingsblankettTask(saksbehandling)
                setOf(IverksettStatus.JOURNALFØRT, IverksettStatus.OK).contains(it) -> opprettLagSaksbehandlingsblankettTask(saksbehandling)
                else -> throw TaskExceptionUtenStackTrace("Mottok status $it fra iverksett for behandlingId=${saksbehandling.id}")
            }
        }
    }

    fun opprettLagSaksbehandlingsblankettTask(saksbehandling: Saksbehandling) {
        taskService.save(LagSaksbehandlingsblankettTask.opprettTask(saksbehandling.id))
    }

    override fun stegType(): StegType = StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
}
