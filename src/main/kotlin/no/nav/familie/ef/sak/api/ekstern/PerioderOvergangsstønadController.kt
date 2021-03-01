package no.nav.familie.ef.sak.api.ekstern

import no.nav.familie.ef.sak.service.PerioderOvergangsstønadService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["/api/ekstern/periode/overgangsstonad"],
                consumes = [APPLICATION_JSON_VALUE],
                produces = [APPLICATION_JSON_VALUE])
@Validated
class PerioderOvergangsstønadController(private val perioderOvergangsstønadService: PerioderOvergangsstønadService) {

    @PostMapping
    @ProtectedWithClaims(issuer = "sts", claimMap = ["sub=srvArena"])
    fun hentPerioder(@RequestBody request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        return perioderOvergangsstønadService.hentPerioder(request)
    }

    @PostMapping("azure")
    @ProtectedWithClaims(issuer = "azuread")
    fun hentPerioderAzure(@RequestBody request: PerioderOvergangsstønadRequest): Ressurs<PerioderOvergangsstønadResponse> {
        return try {
            Ressurs.success(perioderOvergangsstønadService.hentPerioder(request))
        } catch (e: Exception) {
            Ressurs.failure("Henting av perioder for overgangsstønad feilet", error = e)
        }
    }

}
