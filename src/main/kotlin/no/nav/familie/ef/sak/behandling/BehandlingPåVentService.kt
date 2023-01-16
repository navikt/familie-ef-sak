package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.TaAvVentStatus
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BehandlingPåVentService(
    private val behandlingService: BehandlingService,
    private val taskService: TaskService
) {
    @Transactional
    fun settPåVent(behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        brukerfeilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke sette behandling med status ${behandling.status} på vent"
        }

        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.SATT_PÅ_VENT)
        taskService.save(BehandlingsstatistikkTask.opprettVenterTask(behandlingId))
    }

    @Transactional
    fun taAvVent(behandlingId: UUID) {
        when (kanTaAvVent(behandlingId)) {
            TaAvVentStatus.OK -> {}
            TaAvVentStatus.ANNEN_BEHANDLING_MÅ_FERDIGSTILLES ->
                throw ApiFeil(
                    "Annen behandling må ferdigstilles før denne kan aktiveres på nytt",
                    HttpStatus.BAD_REQUEST
                )
            TaAvVentStatus.MÅ_NULSTILLE_VEDTAK -> {
                error("Har ikke støtte for dette ennå")
            }
        }
        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES)
        taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId))
    }

    fun kanTaAvVent(behandlingId: UUID): TaAvVentStatus {
        val behandling = behandlingService.hentBehandling(behandlingId)
        brukerfeilHvis(behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            "Kan ikke ta behandling med status ${behandling.status} av vent"
        }

        val behandlinger = behandlingService.hentBehandlinger(behandling.fagsakId)
            .sortedByDescending { it.sporbar.endret.endretTid }
        if (behandlinger.any { it.id != behandlingId && !it.erAvsluttet() }) {
            return TaAvVentStatus.ANNEN_BEHANDLING_MÅ_FERDIGSTILLES
        }
        val sisteIverksatte = behandlingService.finnSisteIverksatteBehandling(behandling.fagsakId)
        return if (sisteIverksatte == null || sisteIverksatte.id == behandling.forrigeBehandlingId) {
            TaAvVentStatus.OK
        } else {
            TaAvVentStatus.MÅ_NULSTILLE_VEDTAK
        }
    }
}