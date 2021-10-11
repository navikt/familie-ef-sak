package no.nav.familie.ef.sak.ekstern.arena

import no.nav.familie.ef.sak.ekstern.PerioderForBarnetrygdService
import no.nav.familie.kontrakter.felles.PersonIdent
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
@RequestMapping(path = ["/api/ekstern/perioder"],
                consumes = [APPLICATION_JSON_VALUE],
                produces = [APPLICATION_JSON_VALUE])
@Validated
class EksternStønadsperioderController(private val arenaStønadsperioderService: ArenaStønadsperioderService,
                                       private val perioderForBarnetrygdService: PerioderForBarnetrygdService) {

    /**
     * Brukes av Arena
     */
    @PostMapping()
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun hentPerioder(@RequestBody request: PerioderOvergangsstønadRequest): Ressurs<PerioderOvergangsstønadResponse> {
        return try {
            Ressurs.success(arenaStønadsperioderService.hentPerioder(request))
        } catch (e: Exception) {
            Ressurs.failure("Henting av perioder for overgangsstønad feilet", error = e)
        }
    }

    /**
     * Brukes av Barnetrygd, for å vurdere utvidet barnetrygd, henter kun perioder med full overgangsstønad
     */
    @PostMapping("full-overgangsstonad")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun hentPerioderForOvergangsstonad(@RequestBody request: PersonIdent): Ressurs<PerioderOvergangsstønadResponse> {
        return Ressurs.success(perioderForBarnetrygdService.hentPerioderMedFullOvergangsstønad(request))
    }


}
