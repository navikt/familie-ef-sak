package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.ApiFeil
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.JournalføringService
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.kontrakter.felles.journalpost.BrukerIdType
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(path = ["/api/blankett"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BlankettController(private val fagsakService: FagsakService,
                         private val oppgaveService: OppgaveService,
                         private val pdlClient: PdlClient,
private val journalføringService: JournalføringService) {

    @PostMapping("/oppgave/{oppgaveId}")
    fun opprettBlankettBehandling(@PathVariable oppgaveId: Long) {
        oppgaveService.hentEfOppgave(oppgaveId)?.let {
            throw ApiFeil("Det finnes allerede en behandling for denne oppgaven - kan ikke opprettes på nytt", HttpStatus.BAD_REQUEST)
        }
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        require(oppgave.journalpostId != null) { "For å plukke oppgaven må det eksistere en journalpostId" }

        val journalpost = journalføringService.hentJournalpost(oppgave.journalpostId!!)
        val personIdent = journalpost.bruker?.let {
            when (it.type) {
                BrukerIdType.FNR -> it.id
                BrukerIdType.AKTOERID -> pdlClient.hentPersonidenter(it.id).identer.first().ident // TODO: Må vi bytte ut first med gjeldende?
                BrukerIdType.ORGNR -> error("Kan ikke hente journalpost=${journalpost.journalpostId} for orgnr")
            }
        } ?: error("Kan ikke hente journalpost=${journalpost.journalpostId} uten bruker")


        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.OVERGANGSSTØNAD)

// opprett behandling
//        settSøknadPåBehandling(journalpostId, behandling.fagsakId, behandling.id)
//        knyttJournalpostTilBehandling(journalpost, behandling)

    }
}