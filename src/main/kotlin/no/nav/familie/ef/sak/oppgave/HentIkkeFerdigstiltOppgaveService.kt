package no.nav.familie.ef.sak.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service
import java.util.UUID
import no.nav.familie.ef.sak.oppgave.Oppgave as EFOppgave

@Service
class HentIkkeFerdigstiltOppgaveService(
    private val oppgaveClient: OppgaveClient,
    private val oppgaveRepository: OppgaveRepository,
) {

    fun hentIkkeFerdigstiltOppgaveForBehandling(behandlingId: UUID): Oppgave? {
        return hentBehandleSakOppgaveSomIkkeErFerdigstilt(behandlingId)
            ?.let { oppgaveClient.finnOppgaveMedId(it.gsakOppgaveId) }
    }

    fun hentBehandleSakOppgaveSomIkkeErFerdigstilt(behandlingId: UUID): EFOppgave? {
        return oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
            behandlingId,
            setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak),
        )
    }
}
