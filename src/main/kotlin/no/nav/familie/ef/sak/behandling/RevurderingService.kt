package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.steg.StegType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.task.BehandlingsstatistikkTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RevurderingService(private val søknadService: SøknadService,
                         private val behandlingService: BehandlingService,
                         private val oppgaveService: OppgaveService,
                         private val vurderingService: VurderingService,
                         private val grunnlagsdataService: GrunnlagsdataService,
                         private val taskRepository: TaskRepository) {

    fun opprettRevurderingManuelt(fagsakId: UUID): Behandling {
        val revurdering = behandlingService.opprettBehandling(BehandlingType.REVURDERING,
                                                              fagsakId,
                                                              BehandlingStatus.UTREDES,
                                                              StegType.BEREGNE_YTELSE)
        val forrigeBehandlingId = revurdering.forrigeBehandlingId
                                  ?: error("Revurdering må ha eksisterende iverksatt behandling")
        val saksbehandler = SikkerhetContext.hentSaksbehandler(true)

        søknadService.kopierSøknad(forrigeBehandlingId, revurdering.id)
        grunnlagsdataService.opprettGrunnlagsdata(revurdering.id)
        vurderingService.kopierVurderingerTilNyBehandling(forrigeBehandlingId, revurdering.id)
        val oppgaveId = oppgaveService.opprettOppgave(behandlingId = revurdering.id,
                                                      oppgavetype = Oppgavetype.BehandleSak,
                                                      tilordnetNavIdent = saksbehandler,
                                                      beskrivelse = "Revurdering i ny løsning")

        taskRepository.save(BehandlingsstatistikkTask.opprettMottattTask(behandlingId = revurdering.id, oppgaveId = oppgaveId))
        taskRepository.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = revurdering.id))
        return revurdering
    }

}