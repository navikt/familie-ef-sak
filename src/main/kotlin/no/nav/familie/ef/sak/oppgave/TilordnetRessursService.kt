package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
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
    private val featureToggleService: FeatureToggleService,
) {

    /**
     * [SaksbehandlerRolle.OPPGAVE_FINNES_IKKE]: I de tilfellene hvor man manuelt oppretter en revurdering
     * fra fagsakoversikten vil man øyeblikkelig bli redirectet inn i revurderingen. Da har ikke oppgavesystemet
     * rukket å opprette en behandle-sak-oppgave enda. I disse tilfellene ønsker vi å sende med oppgave-finnes-ikke
     * til frontend for å skjule visningen av ansvarlig saksbehandler frem til oppgavesystemet rekker å opprette
     * behandle-sak-oppgaven. Man må kunne redigere frontend i dette tilfellet.
     */
    fun tilordnetRessursErInnloggetSaksbehandler(behandlingId: UUID): Boolean {
        val oppgave = if (erUtviklerMedVeilderrolle()) null else hentIkkeFerdigstiltOppgaveForBehandling(behandlingId)
        val rolle = utledSaksbehandlerRolle(oppgave)

        return when (rolle) {
            SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER, SaksbehandlerRolle.OPPGAVE_FINNES_IKKE -> true
            SaksbehandlerRolle.ANNEN_SAKSBEHANDLER, SaksbehandlerRolle.UTVIKLER_MED_VEILDERROLLE, SaksbehandlerRolle.IKKE_SATT  -> false
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
        if (erUtviklerMedVeilderrolle()) {
            return SaksbehandlerRolle.UTVIKLER_MED_VEILDERROLLE
        }

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

    private fun erUtviklerMedVeilderrolle(): Boolean =
        SikkerhetContext.erSaksbehandler() && featureToggleService.isEnabled(Toggle.UTVIKLER_MED_VEILEDERRROLLE)
}
