package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/uttrekk"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class UttrekkController(private val uttrekkVedtakService: UttrekkArbeidssøkerService) {

    @GetMapping("arbeidssoker")
    fun hentArbeidssøkere(@RequestParam kontrollert: Boolean = false): UttrekkArbeidssøkereDto {
        return uttrekkVedtakService.hentUttrekkArbeidssøkere()
    }

    @PostMapping("arbeidssoker/{id}/kontrollert")
    fun settKontrollert(@PathVariable id: UUID,
                        @RequestParam kontrollert: Boolean = true) {
        uttrekkVedtakService.settKontrollert(id, kontrollert)
    }
}
