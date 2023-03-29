package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/oppgaveforopprettelse")
@ProtectedWithClaims(issuer = "azuread")
class OppgaveForOpprettelseController(private val oppgaverForOpprettelseService: OppgaverForOpprettelseService) {

    @GetMapping("/{behandlingid}")
    fun hentOppgaverForOpprettelse(@PathVariable behandlingid: UUID): Ressurs<OppgaverForOpprettelseDto?> {
        val oppgaverForOpprettelse = oppgaverForOpprettelseService.hentOppgaverForOpprettelseEllerNull(behandlingid)
        return Ressurs.success(
            OppgaverForOpprettelseDto(
                oppgavetyper = oppgaverForOpprettelse?.let { it.oppgavetyper } ?: null,
                kanOppretteOppgaveForInntektAutomatisk = oppgaverForOpprettelseService.kanOppretteOppgaveForInntektskontroll(behandlingid),
            ),
        )
    }
}


