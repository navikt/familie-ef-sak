package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
        path = ["/api/ekstern/behandling"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
)
class EksternBehandlingController(private val pdlClient: PdlClient,
                                  private val eksternBehandlingService: EksternBehandlingService) {

    @PostMapping("finnes")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun finnesBehandlingForPerson(@RequestParam("type") stønadstype: Stønadstype?,
                                  @RequestBody request: PersonIdent): Ressurs<Boolean> {
        val personidenter = pdlClient.hentPersonidenter(request.ident, historikk = true).identer()
        return Ressurs.success(eksternBehandlingService.finnesBehandlingFor(personidenter, stønadstype))
    }

    /**
     * Hvis man har alle identer til en person så kan man sende inn alle direkte, for å unngå oppslag mot pdl
     * Dette er alltså ikke ett bolk-oppslag for flere ulike personer
     */
    @PostMapping("harstonad/flere-identer")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun harStønadSiste12MånederForPersonidenter(@RequestBody personidenter: Set<String>): Ressurs<Boolean> {
        if (personidenter.isEmpty()) {
            return Ressurs.failure("Minst en ident påkrevd for søk")
        }
        if (personidenter.any { it.length != 11 }) {
            return Ressurs.failure("Støtter kun identer av typen fnr/dnr")
        }
        return Ressurs.success(eksternBehandlingService.harStønadSiste12Måneder(personidenter))
    }

}