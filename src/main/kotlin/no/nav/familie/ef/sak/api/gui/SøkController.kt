package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.api.dto.Søkeresultat
import no.nav.familie.ef.sak.exception.PdlNotFoundException
import no.nav.familie.ef.sak.service.SøkService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/sok"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SøkController(private val søkService: SøkService, private val tilgangService: TilgangService) {

    @PostMapping()
    fun søkPerson(@RequestBody personIdentRequest: PersonIdentDto): Ressurs<Søkeresultat> {
        return try {
            tilgangService.validerTilgangTilPersonMedBarn(personIdentRequest.personIdent)
            Ressurs.success(søkService.søkPerson(personIdentRequest.personIdent))
        } catch (e: PdlNotFoundException) {
            Ressurs.failure(frontendFeilmelding = "Finner ikke søkte personen")
        }
    }
}