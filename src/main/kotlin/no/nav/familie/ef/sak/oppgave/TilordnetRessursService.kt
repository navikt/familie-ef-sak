package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.dto.SaksbehandlerDto
import no.nav.familie.ef.sak.oppgave.dto.SaksbehandlerRolle
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.springframework.stereotype.Service
import java.util.UUID
import no.nav.familie.ef.sak.oppgave.Oppgave as EFOppgave

@Service
class TilordnetRessursService(
    private val oppgaveClient: OppgaveClient,
    private val oppgaveRepository: OppgaveRepository,
) {

    fun tilordnetRessursErInnloggetSaksbehandlerEllerNull(behandlingId: UUID): Boolean {
        val tilordnetRessurs = hentIkkeFerdigstiltOppgaveForBehandling(behandlingId)?.tilordnetRessurs
        val rolle = utledSaksbehandlerRolle(tilordnetRessurs)

        return when (rolle) {
            SaksbehandlerRolle.IKKE_SATT, SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER -> true
            SaksbehandlerRolle.ANNEN_SAKSBEHANDLER -> false
        }
    }

    fun hentIkkeFerdigstiltOppgaveForBehandling(behandlingId: UUID): Oppgave? =
        hentBehandleSakOppgaveSomIkkeErFerdigstilt(behandlingId)
            ?.let { oppgaveClient.finnOppgaveMedId(it.gsakOppgaveId) }

    fun hentBehandleSakOppgaveSomIkkeErFerdigstilt(behandlingId: UUID): EFOppgave? =
        oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
            behandlingId,
            setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak),
        )

    fun mapTilSaksbehandlerDto(tilOrdnetRessurs: Saksbehandler?): SaksbehandlerDto {
        val rolle = utledSaksbehandlerRolle(tilOrdnetRessurs?.navIdent)

        return SaksbehandlerDto(
            azureId = tilOrdnetRessurs?.azureId ?: UUID.randomUUID(),
            enhet = tilOrdnetRessurs?.enhet ?: "",
            etternavn = tilOrdnetRessurs?.etternavn ?: "",
            fornavn = tilOrdnetRessurs?.fornavn ?: "",
            navIdent = tilOrdnetRessurs?.navIdent ?: "",
            rolle = rolle,
        )
    }

    private fun utledSaksbehandlerRolle(navIdent: String?): SaksbehandlerRolle {
        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()

        return when (navIdent) {
            innloggetSaksbehandler -> SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER
            null -> SaksbehandlerRolle.IKKE_SATT
            else -> SaksbehandlerRolle.ANNEN_SAKSBEHANDLER
        }
    }
}
