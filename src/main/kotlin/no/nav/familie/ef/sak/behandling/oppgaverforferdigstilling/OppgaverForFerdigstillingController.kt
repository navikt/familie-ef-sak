package no.nav.familie.ef.sak.behandling.oppgaverforferdigstilling

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/oppgaverforferdigstilling")
@ProtectedWithClaims(issuer = "azuread")
class OppgaverForFerdigstillingController(
    private val oppgaverForFerdigstillingService: OppgaverForFerdigstillingService,
) {
    @GetMapping("/{behandlingid}")
    fun hentOppgaverForFerdigstilling(
        @PathVariable behandlingid: UUID,
    ): Ressurs<OppgaverForFerdigstillingDto> {
        val lagretFremleggsoppgaveIder = oppgaverForFerdigstillingService.hentOppgaverForFerdigstillingEllerNull(behandlingid)

        return Ressurs.success(
            OppgaverForFerdigstillingDto(
                behandlingId = behandlingid,
                oppgaveIder =
                    lagretFremleggsoppgaveIder?.fremleggsoppgaveIderSomSkalFerdigstilles
                        ?: emptyList(),
            ),
        )
    }
}
