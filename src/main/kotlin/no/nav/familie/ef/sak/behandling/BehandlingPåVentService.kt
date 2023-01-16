package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.TaAvVentStatus
import no.nav.familie.ef.sak.behandling.dto.TaAvVentStatusDto
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.vedtak.NullstillVedtakService
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BehandlingPåVentService(
    private val behandlingService: BehandlingService,
    private val taskService: TaskService,
    private val nullstillVedtakService: NullstillVedtakService,
    private val featureToggleService: FeatureToggleService
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
        val kanTaAvVent = kanTaAvVent(behandlingId)
        behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES)
        when (kanTaAvVent.status) {
            TaAvVentStatus.OK -> {}
            TaAvVentStatus.ANNEN_BEHANDLING_MÅ_FERDIGSTILLES ->
                throw ApiFeil(
                    "Annen behandling må ferdigstilles før denne kan aktiveres på nytt",
                    HttpStatus.BAD_REQUEST
                )
            TaAvVentStatus.MÅ_NULSTILLE_VEDTAK -> {
                feilHvisIkke(featureToggleService.isEnabled(Toggle.PÅ_VENT_NULLSTILL_VEDTAK)) {
                    "Toggle 'På vent - Nullstill vedtak' er ikke aktivert"
                }
                val nyForrigeBehandlingId = kanTaAvVent.nyForrigeBehandlingId ?: error("Mangler nyForrigeBehandlingId")
                behandlingService.oppdaterForrigeBehandlingId(behandlingId, nyForrigeBehandlingId)
                nullstillVedtakService.nullstillVedtak(behandlingId)
            }
        }
        taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId))
    }

    fun kanTaAvVent(behandlingId: UUID): TaAvVentStatusDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        brukerfeilHvis(behandling.status != BehandlingStatus.SATT_PÅ_VENT) {
            "Kan ikke ta behandling med status ${behandling.status} av vent"
        }

        val behandlinger = behandlingService.hentBehandlinger(behandling.fagsakId)
        if (behandlinger.any { it.id != behandling.id && !it.erAvsluttet() }) {
            return TaAvVentStatusDto(TaAvVentStatus.ANNEN_BEHANDLING_MÅ_FERDIGSTILLES)
        }
        val sisteIverksatte = behandlingService.finnSisteIverksatteBehandling(behandling.fagsakId)
        return if (sisteIverksatte == null || sisteIverksatte.id == behandling.forrigeBehandlingId) {
            TaAvVentStatusDto(TaAvVentStatus.OK)
        } else {
            TaAvVentStatusDto(TaAvVentStatus.MÅ_NULSTILLE_VEDTAK, sisteIverksatte.id)
        }
    }
}