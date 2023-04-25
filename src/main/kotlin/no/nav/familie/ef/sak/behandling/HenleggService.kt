package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseService
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class HenleggService(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val oppgaverForOpprettelseService: OppgaverForOpprettelseService
) {

    @Transactional
    fun henleggBehandling(behandlingId: UUID, henlagt: HenlagtDto): Behandling {
        val behandling = behandlingService.henleggBehandling(behandlingId, henlagt)
        ferdigstillOppgaveTask(behandling)
        oppgaverForOpprettelseService.slettOppgaverForOpprettelse(behandlingId)
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
}
