package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.api.dto.Søkeresultat
import no.nav.familie.ef.sak.api.dto.SøkeresultatPerson
import no.nav.familie.ef.sak.service.SøkService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/sok"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SøkController(private val søkService: SøkService, private val tilgangService: TilgangService) {

    @PostMapping
    fun søkPerson(@RequestBody personIdentRequest: PersonIdentDto): Ressurs<Søkeresultat> {
        tilgangService.validerTilgangTilPersonMedBarn(personIdentRequest.personIdent)
        return Ressurs.success(søkService.søkPerson(personIdentRequest.personIdent))
    }

    @GetMapping("{behandlingId}/samme-adresse")
    fun søkPerson(@PathVariable("behandlingId") behandlingId: UUID): Ressurs<SøkeresultatPerson> {
        return Ressurs.success(søkService.søkEtterPersonerMedSammeAdresse(behandlingId))
    }
}