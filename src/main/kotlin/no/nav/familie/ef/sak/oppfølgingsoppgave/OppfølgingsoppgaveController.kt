package no.nav.familie.ef.sak.oppfølgingsoppgave

import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class OppfølgingsoppgaveDto(
    val behandlingid: UUID,
    val oppgaverForOpprettelse: OppgaverForOpprettelseDto?,
    val oppgaveIderForFerdigstilling: List<Long>?,
    val automatiskBrev: List<String>,
)

@RestController
@RequestMapping("/api/oppfolgingsoppgave")
@ProtectedWithClaims(issuer = "azuread")
class OppfølgingsoppgaveController(
    private val oppfølgingsoppgaveService: OppfølgingsoppgaveService,
) {
    @GetMapping("/{behandlingid}")
    fun hentOppfølgingsoppgave(
        @PathVariable behandlingid: UUID,
    ): Ressurs<OppfølgingsoppgaveDto> {
        val oppgaverForOpprettelse = oppfølgingsoppgaveService.hentOppgaverForOpprettelse(behandlingid)
        val oppgaverForFerdigstilling = oppfølgingsoppgaveService.hentOppgaverForFerdigstilling(behandlingid)
        val automatiskBrev =
            oppfølgingsoppgaveService.hentAutomatiskBrev(behandlingid)

        return Ressurs.success(
            OppfølgingsoppgaveDto(
                behandlingid = behandlingid,
                oppgaverForOpprettelse = oppgaverForOpprettelse,
                oppgaveIderForFerdigstilling = oppgaverForFerdigstilling.oppgaveIder,
                automatiskBrev = automatiskBrev.brevSomSkalSendes,
            ),
        )
    }
}
