package no.nav.familie.ef.sak.behandling.oppgaveforopprettelse

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/oppgaverforopprettelse")
@ProtectedWithClaims(issuer = "azuread")
class OppgaveForOpprettelseController(
    private val oppgaverForOpprettelseService: OppgaverForOpprettelseService,
) {
    @GetMapping("/{behandlingid}")
    fun hentOppgaverForOpprettelse(
        @PathVariable behandlingid: UUID,
    ): Ressurs<OppgaverForOpprettelseDto> {
        val lagretFremleggsoppgave = oppgaverForOpprettelseService.hentOppgaverForOpprettelseEllerNull(behandlingid)
        val oppgavetyperSomKanOpprettes = oppgaverForOpprettelseService.hentOppgavetyperSomKanOpprettes(behandlingid)

        return Ressurs.success(
            OppgaverForOpprettelseDto(
                oppgavetyperSomKanOpprettes = oppgavetyperSomKanOpprettes,
                oppgavetyperSomSkalOpprettes =
                    lagretFremleggsoppgave?.oppgavetyper
                        ?: oppgaverForOpprettelseService.initialVerdierForOppgaverSomSkalOpprettes(behandlingid),
            ),
        )
    }
}
