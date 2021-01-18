package no.nav.familie.ef.sak.api.ekstern

import no.nav.familie.ef.sak.service.PerioderOvergangsstønadService
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping(path = ["/api/ekstern/periode/overgangsstonad"],
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
@Profile("dev") // TODO Kan slettes når det finnes tilgangssjekk. Uklart hvordan denne kommer bli brukt saksbeh/system?
class PerioderOvergangsstønadController(private val perioderOvergangsstønadService: PerioderOvergangsstønadService) {

    @PostMapping
    fun hentPerioder(@RequestBody request: PerioderOvergangsstønadRequest,
                     @RequestParam("delvisOvergangsstonad") boolean: Boolean = false)
            : PerioderOvergangsstønadResponse {
        return perioderOvergangsstønadService.hentPerioder(request)
    }

}
