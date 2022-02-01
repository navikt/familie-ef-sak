package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RevurderingService(private val søknadService: SøknadService,
                         private val behandlingService: BehandlingService,
                         private val oppgaveService: OppgaveService,
                         private val vurderingService: VurderingService,
                         private val grunnlagsdataService: GrunnlagsdataService,
                         private val taskRepository: TaskRepository,
                         private val barnService: BarnService,
                         private val fagsakService: FagsakService) {

    @Transactional
    fun opprettRevurderingManuelt(revurderingInnhold: RevurderingDto): Behandling {
        fagsakService.fagsakMedOppdatertPersonIdent(revurderingInnhold.fagsakId)
        val revurdering = behandlingService.opprettBehandling(BehandlingType.REVURDERING,
                                                              revurderingInnhold.fagsakId,
                                                              BehandlingStatus.UTREDES,
                                                              StegType.BEREGNE_YTELSE,
                                                              revurderingInnhold.behandlingsårsak,
                                                              revurderingInnhold.kravMottatt)
        val forrigeBehandlingId = forrigeBehandling(revurdering)
        val saksbehandler = SikkerhetContext.hentSaksbehandler(true)

        søknadService.kopierSøknad(forrigeBehandlingId, revurdering.id)
        grunnlagsdataService.opprettGrunnlagsdata(revurdering.id)

        // TODO: Må kunne ta imot en liste med barn
        barnService.opprettBarnPåBehandlingMedSøknadsdata(revurdering.id, revurdering.fagsakId)
        vurderingService.kopierVurderingerTilNyBehandling(forrigeBehandlingId, revurdering.id)
        val oppgaveId = oppgaveService.opprettOppgave(behandlingId = revurdering.id,
                                                      oppgavetype = Oppgavetype.BehandleSak,
                                                      tilordnetNavIdent = saksbehandler,
                                                      beskrivelse = "Revurdering i ny løsning")

        taskRepository.save(BehandlingsstatistikkTask.opprettMottattTask(behandlingId = revurdering.id, oppgaveId = oppgaveId))
        taskRepository.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = revurdering.id))
        return revurdering
    }

    /**
     * Returnerer id til forrige behandling.
     * Skal håndtere en førstegangsbehandling som er avslått, då vi trenger en behandlingId for å kopiere data fra søknaden
     */
    private fun forrigeBehandling(revurdering: Behandling): UUID {
        val sisteBehandling = behandlingService.hentBehandlinger(revurdering.fagsakId)
                .filter { it.id != revurdering.id }
                .filter { it.resultat != BehandlingResultat.HENLAGT }
                .maxByOrNull { it.sporbar.opprettetTid }
        return revurdering.forrigeBehandlingId
               ?: sisteBehandling?.id
               ?: error("Revurdering må ha eksisterende iverksatt behandling")
    }

}