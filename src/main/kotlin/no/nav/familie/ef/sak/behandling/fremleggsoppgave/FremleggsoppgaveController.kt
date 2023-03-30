package no.nav.familie.ef.sak.behandling.fremleggsoppgave

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/fremleggsoppgave")
@ProtectedWithClaims(issuer = "azuread")
class FremleggsoppgaveController(private val fremleggsoppgaveService: FremleggsoppgaveService) {

    @GetMapping("/{behandlingId}")
    fun hentFremleggsoppgave(@PathVariable behandlingId: UUID): Ressurs<FremleggWrapper> {
        val lagretFremleggsoppgave = fremleggsoppgaveService.hentFremleggsoppgave(behandlingId)
        return Ressurs.success(
            FremleggWrapper(
                oppgavetyperSomKanOpprettes = fremleggsoppgaveService.hentOppgavetyperSomKanOpprettes(behandlingId),
                oppgavetyperSomSkalOpprettes = lagretFremleggsoppgave?.oppgavetyper ?: emptyList(),
                opprettelseTattStillingTil = lagretFremleggsoppgave?.opprettelseTattStillingTil ?: false
            ),
        )
    }
}
