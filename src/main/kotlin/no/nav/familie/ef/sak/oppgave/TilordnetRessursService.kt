package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggle
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
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
     *  I tilfeller hvor saksbehandler manuelt oppretter en revurdering eller en førstegangsbehandling vil oppgaven
     *  som returneres fra oppgavesystemet være null. Dette skjer fordi oppgavesystemet bruker litt tid av variabel
     *  lengde på å opprette den tilhørende behandle-sak-oppgaven til den opprettede behandlingen.
     *
     * [SaksbehandlerRolle.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER]: Dersom null blir returnert og
     * behandlingen befinner seg i steget REVURDERING_ÅRSAK, VILKÅR eller BEREGNE_YTELSE anser vi det som svært
     * sannsynlig at det er den innloggede saksbehandleren som er ansvarlig for behandlingen - oppgaven har bare ikke
     * rukket å bli opprettet enda. I dette tilfellet returnerer vi OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER
     * til frontend.
     *
     * [SaksbehandlerRolle.OPPGAVE_FINNES_IKKE]: Dersom null returneres og behandlingen ikke befinner seg i et av de nevnte
     * stegene returnerer vi OPPGAVE_FINNES_IKKE til frontend.
     */
    fun tilordnetRessursErInnloggetSaksbehandler(
        behandlingId: UUID,
        oppgavetyper: Set<Oppgavetype> = setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak),
    ): Boolean {
        val oppgave = if (erUtviklerMedVeilderrolle()) null else hentIkkeFerdigstiltOppgaveForBehandling(behandlingId, oppgavetyper)
        val rolle = utledSaksbehandlerRolle(behandlingId, oppgave)

        return when (rolle) {
            SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER, SaksbehandlerRolle.OPPGAVE_FINNES_IKKE, SaksbehandlerRolle.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER -> true
            else -> false
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

    fun utledAnsvarligSaksbehandlerForOppgave(
        behandlingId: UUID,
        behandleSakOppgave: Oppgave?,
    ): SaksbehandlerDto {
        val rolle = utledSaksbehandlerRolle(behandlingId, behandleSakOppgave)

        val tilordnetRessurs =
            if (rolle == SaksbehandlerRolle.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER) {
                hentSaksbehandlerInfo(SikkerhetContext.hentSaksbehandler())
            } else {
                behandleSakOppgave?.tilordnetRessurs?.let { hentSaksbehandlerInfo(it) }
            }

        return SaksbehandlerDto(
            etternavn = tilordnetRessurs?.etternavn ?: "",
            fornavn = tilordnetRessurs?.fornavn ?: "",
            rolle = rolle,
        )
    }

    fun hentSaksbehandlerInfo(navIdent: String) = oppgaveClient.hentSaksbehandlerInfo(navIdent)

    private fun utledSaksbehandlerRolle(
        behandlingId: UUID,
        oppgave: Oppgave?,
    ): SaksbehandlerRolle {
        if (erUtviklerMedVeilderrolle()) {
            return SaksbehandlerRolle.UTVIKLER_MED_VEILDERROLLE
        }

        if (oppgave == null) {
            val behandling = behandlingRepository.findByIdOrThrow(behandlingId)

            if (behandling.steg == StegType.REVURDERING_ÅRSAK || behandling.steg == StegType.VILKÅR || behandling.steg == StegType.BEREGNE_YTELSE) {
                return SaksbehandlerRolle.OPPGAVE_FINNES_IKKE_SANNSYNLIGVIS_INNLOGGET_SAKSBEHANDLER
            }
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

    private fun erUtviklerMedVeilderrolle(): Boolean = featureToggleService.isEnabled(FeatureToggle.UtviklerMedVeilederrolle)
}
