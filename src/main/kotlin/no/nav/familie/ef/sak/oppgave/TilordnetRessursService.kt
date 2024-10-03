package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.dto.SaksbehandlerDto
import no.nav.familie.ef.sak.oppgave.dto.SaksbehandlerRolle
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum
import org.springframework.stereotype.Service
import java.util.UUID
import no.nav.familie.ef.sak.oppgave.Oppgave as EFOppgave

@Service
class TilordnetRessursService(
    private val oppgaveClient: OppgaveClient,
    private val oppgaveRepository: OppgaveRepository,
    private val featureToggleService: FeatureToggleService,
    private val behandlingRepository: BehandlingRepository,
) {
    /**
     * [SaksbehandlerRolle.OPPGAVE_FINNES_IKKE]: I de tilfellene hvor man manuelt oppretter en revurdering
     * fra fagsakoversikten vil man øyeblikkelig bli redirectet inn i revurderingen. Da har ikke oppgavesystemet
     * rukket å opprette en behandle-sak-oppgave enda. I disse tilfellene ønsker vi å sende med oppgave-finnes-ikke
     * til frontend for å skjule visningen av ansvarlig saksbehandler frem til oppgavesystemet rekker å opprette
     * behandle-sak-oppgaven. Man må kunne redigere frontend i dette tilfellet.
     */
    fun tilordnetRessursErInnloggetSaksbehandler(
        behandlingId: UUID,
        oppgavetyper: Set<Oppgavetype> = setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak),
    ): Boolean {
        val oppgave = if (erUtviklerMedVeilderrolle()) null else hentIkkeFerdigstiltOppgaveForBehandling(behandlingId, oppgavetyper)
        val rolle = utledSaksbehandlerRolle(oppgave)

        return when (rolle) {
            SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER, SaksbehandlerRolle.OPPGAVE_FINNES_IKKE -> true
            SaksbehandlerRolle.ANNEN_SAKSBEHANDLER, SaksbehandlerRolle.UTVIKLER_MED_VEILDERROLLE, SaksbehandlerRolle.OPPGAVE_TILHØRER_IKKE_ENF, SaksbehandlerRolle.IKKE_SATT -> false
        }
    }

    fun hentIkkeFerdigstiltOppgaveForBehandling(
        behandlingId: UUID,
        oppgavetyper: Set<Oppgavetype> = setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak, Oppgavetype.GodkjenneVedtak),
    ): Oppgave? =
        hentEFOppgaveSomIkkeErFerdigstilt(behandlingId, oppgavetyper)
            ?.let { oppgaveClient.finnOppgaveMedId(it.gsakOppgaveId) }

    fun hentIkkeFerdigstiltOppgaveForBehandlingGittStegtype(
        behandlingId: UUID,
    ): Oppgave? {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val oppgavetyper =
            when (behandling.steg.tillattFor) {
                BehandlerRolle.SAKSBEHANDLER -> setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak)
                BehandlerRolle.BESLUTTER -> setOf(Oppgavetype.GodkjenneVedtak)
                else -> emptySet()
            }

        return hentEFOppgaveSomIkkeErFerdigstilt(behandlingId, oppgavetyper)
            ?.let { oppgaveClient.finnOppgaveMedId(it.gsakOppgaveId) }
    }

    fun hentEFOppgaveSomIkkeErFerdigstilt(
        behandlingId: UUID,
        oppgavetyper: Set<Oppgavetype>,
    ): EFOppgave? =
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
        if (erUtviklerMedVeilderrolle()) {
            return SaksbehandlerRolle.UTVIKLER_MED_VEILDERROLLE
        }

        if (oppgave == null) {
            return SaksbehandlerRolle.OPPGAVE_FINNES_IKKE
        }

        if (oppgave.tema != Tema.ENF || oppgave.status == StatusEnum.FEILREGISTRERT) {
            return SaksbehandlerRolle.OPPGAVE_TILHØRER_IKKE_ENF
        }

        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        return when (oppgave.tilordnetRessurs) {
            innloggetSaksbehandler -> SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER
            null -> SaksbehandlerRolle.IKKE_SATT
            else -> SaksbehandlerRolle.ANNEN_SAKSBEHANDLER
        }
    }

    private fun erUtviklerMedVeilderrolle(): Boolean =
        featureToggleService.isEnabled(Toggle.UTVIKLER_MED_VEILEDERRROLLE)
}
