package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.api.ApiFeil
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.*
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/blankett"])
@ProtectedWithClaims(issuer = "azuread")
@Unprotected
class BlankettController(private val tilgangService: TilgangService,
                         private val vurderingService: VurderingService,
                         private val blankettClient: BlankettClient,
                         private val blankettService: BlankettService,
                         private val oppgaveService: OppgaveService,
                         private val journalføringService: JournalføringService,
                         private val personService: PersonService,
                         private val behandlingService: BehandlingService,
                         private val fagsakService: FagsakService,
                         private val personopplysningerService: PersonopplysningerService) {

    @PostMapping("{behandlingId}")
    fun lagBlankettPdf(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val blankettPdfRequest = BlankettPdfRequest(lagPersonopplysningerDto(behandlingId),
                                                    hentInngangsvilkårDto(behandlingId))
        val blankett = blankettClient.genererBlankett(blankettPdfRequest)
        blankettService.oppdaterBlankett(behandlingId, blankett)
        return Ressurs.success(blankett)
    }

    @PostMapping("/oppgave/{oppgaveId}")
    fun opprettBlankettBehandling(@PathVariable oppgaveId: Long): Ressurs<UUID> {
        oppgaveService.hentEfOppgave(oppgaveId)?.let {
            throw ApiFeil("Det finnes allerede en behandling for denne oppgaven - kan ikke opprettes på nytt",
                          HttpStatus.BAD_REQUEST)
        }
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        require(oppgave.journalpostId != null) { "For å plukke oppgaven må det eksistere en journalpostId" }

        val journalpost = journalføringService.hentJournalpost(oppgave.journalpostId!!)
        val personIdent = personService.hentIdentForJournalpost(journalpost)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent)
        val søknad = journalføringService.hentSøknadFraJournalpostForOvergangsstønad(oppgave.journalpostId.toString())
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.OVERGANGSSTØNAD)
        val behandling = behandlingService.opprettBehandling(BehandlingType.BLANKETT, fagsak.id, søknad, journalpost)

        return Ressurs.success(behandling.id)
    }


    private fun hentInngangsvilkårDto(behandlingId: UUID) = vurderingService.hentInngangsvilkår(behandlingId)

    private fun lagPersonopplysningerDto(behandlingId: UUID): PersonopplysningerDto {
        val ident = fagsakService.hentFagsak(behandlingService.hentBehandling(behandlingId).fagsakId).hentAktivIdent()
        return PersonopplysningerDto(hentGjeldendeNavn(ident), ident)
    }

    private fun hentGjeldendeNavn(hentAktivIdent: String): String {
        val navnMap = personopplysningerService.hentGjeldeneNavn(listOf(hentAktivIdent))
        return navnMap.getValue(hentAktivIdent)
    }
}