package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RevurderingService(private val søknadService: SøknadService,
                         private val behandlingService: BehandlingService,
                         private val oppgaveService: OppgaveService,
                         private val vurderingService: VurderingService,
                         private val grunnlagsdataService: GrunnlagsdataService) {

    fun opprettRevurderingManuelt(fagsakId: UUID): Behandling {
        val sisteIverksatteBehandlingUUID = behandlingService.finnSisteIverksatteBehandling(fagsakId)
                                            ?: error("Revurdering må ha eksisterende iverksatt behandling")
        val revurdering = behandlingService.opprettBehandling(BehandlingType.REVURDERING, fagsakId)
        val saksbehandler = SikkerhetContext.hentSaksbehandler(true)
        /**
         * Når man revurderer basert på en ny søknad så kan man ikke bruke denne metoden hvis den kopierer forrige søknad
         */
        søknadService.kopierSøknad(sisteIverksatteBehandlingUUID, revurdering.id)
        grunnlagsdataService.opprettGrunnlagsdata(revurdering.id)
        vurderingService.kopierVurderingerTilNyBehandling(sisteIverksatteBehandlingUUID, revurdering.id)

        oppgaveService.opprettOppgave(behandlingId= revurdering.id,
                                      oppgavetype = Oppgavetype.BehandleSak,
                                      tilordnetNavIdent = saksbehandler,
                                      beskrivelse = "Revurdering i ny løsning")

        //TODO opprettBehandlingsstatistikkTask(revurdering.id, oppgaveId.toLong())
        return revurdering
    }


}