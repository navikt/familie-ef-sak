package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Skal vi returnere en liste med FNR ? link til behandling?
 * Hvordan gjør vi med de som er kode 6/7?
 */

@RestController
@RequestMapping(path = ["/api/uttrekk/vedtak"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class UttrekkVedtakController(private val tilgangService: TilgangService) {


    @GetMapping("arbeidssoker")
    fun hentArbeidssøkere() {

    }
}
