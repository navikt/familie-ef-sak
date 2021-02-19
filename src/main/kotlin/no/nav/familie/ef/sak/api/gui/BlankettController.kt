package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.ApiFeil
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.JournalføringService
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.PersonService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.journalpost.BrukerIdType
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/blankett"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BlankettController(private val fagsakService: FagsakService,
                         private val oppgaveService: OppgaveService,
                         private val behandlingService: BehandlingService,
                         private val personService: PersonService,
                         private val tilgangService: TilgangService,
                         private val journalføringService: JournalføringService) {

    @PostMapping("/oppgave/{oppgaveId}")
    fun opprettBlankettBehandling(@PathVariable oppgaveId: Long): UUID {
        oppgaveService.hentEfOppgave(oppgaveId)?.let {
            throw ApiFeil("Det finnes allerede en behandling for denne oppgaven - kan ikke opprettes på nytt",
                          HttpStatus.BAD_REQUEST)
        }
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        require(oppgave.journalpostId != null) { "For å plukke oppgaven må det eksistere en journalpostId" }

        val journalpost = journalføringService.hentJournalpost(oppgave.journalpostId!!)
        val personIdent = personService.hentIdentForJournalpost(journalpost)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent)
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.OVERGANGSSTØNAD)
        val behandling = behandlingService.opprettBehandling(BehandlingType.BLANKETT, fagsak.id)
        journalføringService.settSøknadPåBehandling(oppgave.journalpostId.toString(), behandling.fagsakId, behandling.id)
        journalføringService.knyttJournalpostTilBehandling(journalpost, behandling)

        return behandling.id
    }
}