package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class HenleggService(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
) {
    @Transactional
    fun henleggBehandling(
        behandlingId: UUID,
        henlagt: HenlagtDto,
    ): Behandling {
        val behandling = behandlingService.henleggBehandling(behandlingId, henlagt)
        ferdigstillOppgaveTask(behandling)
        return behandling
    }

    @Transactional
    fun henleggBehandlingUtenOppgave(
        behandlingId: UUID,
        henlagt: HenlagtDto,
    ): Behandling {
        val behandling = behandlingService.henleggBehandling(behandlingId, henlagt, false)
        settEfOppgaveTilFerdig(behandling)
        return behandling
    }

    private fun ferdigstillOppgaveTask(behandling: Behandling) {
        oppgaveService.ferdigstillOppgaveHvisOppgaveFinnes(
            behandlingId = behandling.id,
            Oppgavetype.BehandleSak,
            ignorerFeilregistrert = true,
        )
        oppgaveService.ferdigstillOppgaveHvisOppgaveFinnes(
            behandlingId = behandling.id,
            Oppgavetype.BehandleUnderkjentVedtak,
            ignorerFeilregistrert = true,
        )
    }

    private fun settEfOppgaveTilFerdig(behandling: Behandling) {
        oppgaveService.settEfOppgaveTilFerdig(
            behandlingId = behandling.id,
            Oppgavetype.BehandleSak,
        )
        oppgaveService.settEfOppgaveTilFerdig(
            behandlingId = behandling.id,
            Oppgavetype.BehandleUnderkjentVedtak,
        )
    }
}
