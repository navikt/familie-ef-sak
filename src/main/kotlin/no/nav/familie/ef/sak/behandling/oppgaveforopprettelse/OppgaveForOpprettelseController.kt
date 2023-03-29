package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

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
class OppgaveForOpprettelseController(private val fremleggspppgaveService: OppgaverForOpprettelseService) {

    @GetMapping("/{behandlingid}")
    fun hentFremleggsoppgave(@PathVariable behandlingid: UUID): Ressurs<OppgaverForOpprettelseDto?> {
        val fremleggsOppgave = fremleggspppgaveService.hentFremleggsoppgave(behandlingid)
        return Ressurs.success(
            OppgaverForOpprettelseDto(
                oppgavetyper = fremleggsOppgave?.let { it.oppgavetyper } ?: null,
                kanOppretteOppgaveForInntektAutomatisk = fremleggspppgaveService.kanOpprettes(behandlingid),
            ),
        )
    }
}
