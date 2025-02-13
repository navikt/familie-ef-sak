package no.nav.familie.ef.sak.oppfølgingsoppgave

import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseDto
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
@Deprecated("Brukes for bakoverkompatibilitet - Bruk OppfølgingsoppgaveController")
class OppgaveForOpprettelseController(
    private val oppfølgingsoppgaveService: OppfølgingsoppgaveService,
) {
    @GetMapping("/{behandlingid}")
    fun hentOppgaverForOpprettelse(
        @PathVariable behandlingid: UUID,
    ): Ressurs<OppgaverForOpprettelseDto> {
        val lagretFremleggsoppgave = oppfølgingsoppgaveService.hentOppgaverForOpprettelseEllerNull(behandlingid)
        val oppgavetyperSomKanOpprettes = oppfølgingsoppgaveService.hentOppgavetyperSomKanOpprettes(behandlingid)

        return Ressurs.success(
            OppgaverForOpprettelseDto(
                oppgavetyperSomKanOpprettes = oppgavetyperSomKanOpprettes,
                oppgavetyperSomSkalOpprettes =
                    lagretFremleggsoppgave?.oppgavetyper
                        ?: oppfølgingsoppgaveService.initialVerdierForOppgaverSomSkalOpprettes(behandlingid),
            ),
        )
    }
}
