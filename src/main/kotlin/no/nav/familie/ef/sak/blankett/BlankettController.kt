package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.api.ApiFeil
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.TilgangService
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
                         private val blankettService: BlankettService,
                         private val oppgaveService: OppgaveService) {

    @PostMapping("{behandlingId}")
    fun lagBlankettPdf(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val blankett = blankettService.lagBlankett(behandlingId)
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

        val behandling = blankettService.opprettBlankettBehandling(oppgave.journalpostId!!)

        return Ressurs.success(behandling.id)
    }
}