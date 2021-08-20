package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RevurderingService(private val søknadService: SøknadService,
                         private val behandlingService: BehandlingService,
                         private val oppgaveService: OppgaveService) {

    fun opprettRevurderingManuelt(fagsakId: UUID) {
        val sisteIverksatteBehandlingUUID = behandlingService.finnSisteIverksatteBehandling(fagsakId)
                                            ?: error("Revurdering må ha eksisterende iverksatt behandling")
        val revurdering = behandlingService.opprettBehandling(BehandlingType.REVURDERING, fagsakId)
        val saksbehandler = SikkerhetContext.hentSaksbehandler(true)
        /**
         * Når man revurderer basert på en ny søknad så kan man ikke bruke denne metoden hvis den kopierer forrige søknad
         */
        søknadService.kopierOvergangsstønad(sisteIverksatteBehandlingUUID, revurdering.id)
        //vilkår
        //personopplysninger

        val oppgave = oppgaveService.opprettOppgave(revurdering.id,
                                                    Oppgavetype.BehandleSak,
                                                    saksbehandler,
                                                    "Revurdering i ny løsning") // TODO 1. kan vi ha en type revurdering? 2. Bedre beskrivelse?
    }


}