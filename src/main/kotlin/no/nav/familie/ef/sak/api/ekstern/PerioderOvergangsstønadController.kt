package no.nav.familie.ef.sak.api.ekstern

import no.nav.familie.ef.sak.service.PerioderOvergangsstønadService
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*


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

}
