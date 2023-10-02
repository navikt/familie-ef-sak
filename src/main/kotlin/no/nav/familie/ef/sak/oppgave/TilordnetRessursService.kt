package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.dto.SaksbehandlerDto
import no.nav.familie.ef.sak.oppgave.dto.SaksbehandlerRolle
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
        val oppgave = hentIkkeFerdigstiltOppgaveForBehandling(behandlingId)
        val rolle = utledSaksbehandlerRolle(oppgave)

        return when (rolle) {
            SaksbehandlerRolle.IKKE_SATT, SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER, SaksbehandlerRolle.OPPGAVE_FINNES_IKKE -> true
            SaksbehandlerRolle.ANNEN_SAKSBEHANDLER -> false
        }
    }

    fun hentIkkeFerdigstiltOppgaveForBehandling(behandlingId: UUID, oppgavetyper: Set<Oppgavetype>? = null): Oppgave? {
        val typer = if (oppgavetyper.isNullOrEmpty()) setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak) else oppgavetyper

        return hentEFOppgaveSomIkkeErFerdigstilt(behandlingId, typer)
            ?.let { oppgaveClient.finnOppgaveMedId(it.gsakOppgaveId) }
    }

    fun hentEFOppgaveSomIkkeErFerdigstilt(behandlingId: UUID, oppgavetyper: Set<Oppgavetype>): EFOppgave? =
        oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
            behandlingId,
            oppgavetyper,
        )

    fun utledAnsvarligSaksbehandlerForOppgave(behandleSakOppgave: Oppgave?): SaksbehandlerDto {
        val tilOrdnetRessurs = behandleSakOppgave?.tilordnetRessurs?.let { hentSaksbehandlerInfo(it) }
        val rolle = utledSaksbehandlerRolle(behandleSakOppgave)

        return SaksbehandlerDto(
            etternavn = tilOrdnetRessurs?.etternavn ?: "",
            fornavn = tilOrdnetRessurs?.fornavn ?: "",
            rolle = rolle,
        )
    }

    fun hentSaksbehandlerInfo(navIdent: String) = oppgaveClient.hentSaksbehandlerInfo(navIdent)

    private fun utledSaksbehandlerRolle(oppgave: Oppgave?): SaksbehandlerRolle {
        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()

        if (oppgave == null) {
            return SaksbehandlerRolle.OPPGAVE_FINNES_IKKE
        }

        return when (oppgave.tilordnetRessurs) {
            innloggetSaksbehandler -> SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER
            null -> SaksbehandlerRolle.IKKE_SATT
            else -> SaksbehandlerRolle.ANNEN_SAKSBEHANDLER
        }
    }
}
