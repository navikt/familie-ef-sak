package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.api.dto.SakSøkDto
import no.nav.familie.ef.sak.api.dto.SakSøkListeDto
import no.nav.familie.ef.sak.service.SakSøkService
import no.nav.familie.ef.sak.validering.PersontilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/saksok")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SakSøkController(private val sakSøkService: SakSøkService) {

    // tilgang gjøres inne i finnSaker
    @GetMapping
    fun finnSaker(): Ressurs<SakSøkListeDto> {
        return sakSøkService.finnSaker()
    }

    @PostMapping("ident")
    fun søkMedIdent(@PersontilgangConstraint @RequestBody identParam: PersonIdentDto): Ressurs<SakSøkDto> {
        return sakSøkService.finnSakForPerson(identParam.personIdent)
    }

}
