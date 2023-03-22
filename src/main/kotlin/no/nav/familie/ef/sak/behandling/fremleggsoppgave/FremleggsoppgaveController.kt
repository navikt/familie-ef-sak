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

    @GetMapping("/{behandlingid}")
    fun hentFremleggsoppgaveForInntekt(@PathVariable behandlingid: UUID): Ressurs<FremleggsoppgaveDto?> {
        val fremleggsOppgave = fremleggspppgaveService.hentFremleggsoppgave(behandlingid)
        val opprettFremleggsoppgave = fremleggsOppgave?.let { it.inntekt } ?: null
        val kanOppretteFremleggsoppgave = fremleggspppgaveService.kanOpprettes(behandlingid)
        return Ressurs.success(
            FremleggsoppgaveDto(
                skalOpprettFremleggsoppgave = opprettFremleggsoppgave,
                kanOppretteFremleggsoppgave = kanOppretteFremleggsoppgave
            )
        )
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