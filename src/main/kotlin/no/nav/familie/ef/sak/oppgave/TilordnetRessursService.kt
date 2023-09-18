package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service
import java.util.UUID
import no.nav.familie.ef.sak.oppgave.Oppgave as EFOppgave

@Service
class TilordnetRessursService(
    private val oppgaveClient: OppgaveClient,
    private val oppgaveRepository: OppgaveRepository,
) {

    fun tilordnetRessursErInnloggetSaksbehandlerEllerNull(behandlingId: UUID): Boolean {
        println("LOGGER HER FØR KALL")
        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        val tilordnetRessurs = hentIkkeFerdigstiltOppgaveForBehandling(behandlingId)?.tilordnetRessurs
        println("LOGGER HER ETTER KALL")
        println(innloggetSaksbehandler)
        println(tilordnetRessurs)


        return tilordnetRessurs == null || tilordnetRessurs == innloggetSaksbehandler
    }

    fun hentIkkeFerdigstiltOppgaveForBehandling(behandlingId: UUID): Oppgave? =
        hentBehandleSakOppgaveSomIkkeErFerdigstilt(behandlingId)
            ?.let { oppgaveClient.finnOppgaveMedId(it.gsakOppgaveId) }

    fun hentBehandleSakOppgaveSomIkkeErFerdigstilt(behandlingId: UUID): EFOppgave? =
        oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
            behandlingId,
            setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak))
}
