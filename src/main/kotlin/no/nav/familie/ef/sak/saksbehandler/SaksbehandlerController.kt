package no.nav.familie.ef.sak.saksbehandler

import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/saksbehandler")
@ProtectedWithClaims(issuer = "azuread")
class SaksbehandlerController(
    private val oppgaveClient: OppgaveClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/behandler-informasjon")
    fun hentSaksbehandlerInformasjon(
        @RequestBody requestBody: HentSaksbehandlerRequest
    ): Ressurs<Saksbehandler> {
        logger.info("Henter saksbehandler informasjon for: $requestBody.navIdent")
        val saksbehandler = oppgaveClient.hentSaksbehandlerInfo(requestBody.navIdent)
        return Ressurs.success(saksbehandler)
    }
}

data class HentSaksbehandlerRequest(
    val navIdent: String,
)