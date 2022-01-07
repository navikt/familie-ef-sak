package no.nav.familie.ef.sak.organisasjon

import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.ereg.EregService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/organisasjon")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OrganisasjonController(val eregService: EregService) {

    @GetMapping("/{organisasjonsnummer}")
    fun hentOrganisasjon(@PathVariable organisasjonsnummer: String): Ressurs<Organisasjon> {
        if (!ORGNR_REGEX.matches(organisasjonsnummer)) {
            throw ApiFeil("Ugyldig organisasjonsnummer", HttpStatus.BAD_REQUEST)
        }
        return Ressurs.success(eregService.hentOrganisasjon(organisasjonsnummer))
    }

    private val ORGNR_REGEX = """\d{9}""".toRegex()
}