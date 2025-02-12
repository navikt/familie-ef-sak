package no.nav.familie.ef.sak.behandling.oppfølgingsoppgave

import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseDto
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class OppfølgingsoppgaveDto(
    val behandlingid: UUID,
    val oppgaverForOpprettelse: OppgaverForOpprettelseDto?,
    val oppgaveIderForFerdigstilling: List<Long>?,
)

@RestController
@RequestMapping("/api/oppfølgingsoppgave")
class OppfølgingsoppgaveController(
    private val oppfølgingsoppgaveService: OppfølgingsoppgaveService,
) {
    @GetMapping("/{behandlingid}")
    fun hentOppfølgingsoppgave(
        @PathVariable behandlingid: UUID,
    ): Ressurs<OppfølgingsoppgaveDto> {
        val oppgaverForOpprettelse = oppfølgingsoppgaveService.hentOppgaverForOpprettelse(behandlingid)
        val oppgaverForFerdigstilling = oppfølgingsoppgaveService.hentOppgaverForFerdigstilling(behandlingid)

        return Ressurs.success(
            OppfølgingsoppgaveDto(
                behandlingid = behandlingid,
                oppgaverForOpprettelse = oppgaverForOpprettelse,
                oppgaveIderForFerdigstilling = oppgaverForFerdigstilling.oppgaveIder,
            ),
        )
    }
}
