package no.nav.familie.ef.sak.behandling.fremleggsoppgave

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/fremleggsoppgave")
@ProtectedWithClaims(issuer = "azuread")
class FremleggsoppgaveController(private val fremleggspppgaveService: FremleggsoppgaveService) {

    @GetMapping("{behandlingid}/kan-opprettes")
    fun kanOppretteFremleggsoppgave(@PathVariable behandlingid: UUID): Ressurs<Boolean> {
        return Ressurs.success(fremleggspppgaveService.kanOpprettes(behandlingid))
    }

    @GetMapping("/{behandlingid}")
    fun hentFremleggsoppgave(@PathVariable behandlingid: UUID): Ressurs<FremleggsoppgaveDto?> {
        return Ressurs.success(fremleggspppgaveService.hentFremleggsoppgave(behandlingid)?.tilDto())
    }

    @PostMapping("/{behandlingid}/inntekt/{skalopprette}")
    fun opprettFremleggsoppgave(
        @PathVariable behandlingid: UUID,
        @PathVariable skalopprette: Boolean
    ): Ressurs<Unit> {
        return Ressurs.success(
            fremleggspppgaveService.opprettEllerErstattFremleggsoppgave(
                behandlingid,
                skalopprette
            )
        )
    }
}