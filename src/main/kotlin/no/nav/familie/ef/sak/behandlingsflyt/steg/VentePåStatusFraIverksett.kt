package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.task.LagSaksbehandlingsblankettTask
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import org.springframework.stereotype.Service

@Service
class VentePåStatusFraIverksett(private val iverksettClient: IverksettClient,
                                private val tilkjentYtelseService: TilkjentYtelseService,
                                private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        iverksettClient.hentStatus(saksbehandling.id).let {
            when {
                erMigreringOgOk(saksbehandling, it) -> opprettLagSaksbehandlingsblankettTask(saksbehandling)
                it == IverksettStatus.OK -> opprettLagSaksbehandlingsblankettTask(saksbehandling)
                else -> throw TaskExceptionUtenStackTrace("Mottok status $it fra iverksett for behandlingId=${saksbehandling.id}")
            }
        }
    }

    private fun erMigreringOgOk(saksbehandling: Saksbehandling,
                                it: IverksettStatus): Boolean {
        val erMigrering = saksbehandling.erMigrering()
        if (!erMigrering) {
            return false
        }
        return it == IverksettStatus.OK_MOT_OPPDRAG ||
               (it == IverksettStatus.SENDT_TIL_OPPDRAG && gjelderBehandlingMed0beløp(saksbehandling))
    }

    private fun gjelderBehandlingMed0beløp(saksbehandling: Saksbehandling) =
            tilkjentYtelseService.hentForBehandling(saksbehandling.id).andelerTilkjentYtelse.all { it.beløp == 0 }

    fun opprettLagSaksbehandlingsblankettTask(saksbehandling: Saksbehandling) {
        taskRepository.save(LagSaksbehandlingsblankettTask.opprettTask(saksbehandling.id))
    }

    override fun stegType(): StegType {
        return StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
    }

}