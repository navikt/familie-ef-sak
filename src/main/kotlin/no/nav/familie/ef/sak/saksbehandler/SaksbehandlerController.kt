package no.nav.familie.ef.sak.saksbehandler

import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/saksbehandler")
@ProtectedWithClaims(issuer = "azuread")
class SaksbehandlerController(
    private val oppgaveClient: OppgaveClient,
) {
    private val logger = Logg.getLogger(this::class)

    @GetMapping("/saksbehandler-informasjon")
    fun hentSaksbehandlerInformasjon(): Ressurs<Saksbehandler> {
        val navIdent = SikkerhetContext.hentSaksbehandler()
        logger.info("Henter saksbehandler informasjon for: $navIdent")

        val saksbehandler = oppgaveClient.hentSaksbehandlerInfo(navIdent)
        return Ressurs.success(saksbehandler)
    }
}
