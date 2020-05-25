package no.nav.familie.ef.sak.api

import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.api.dto.SakSøkDto
import no.nav.familie.ef.sak.service.SakSøkService
import no.nav.familie.ef.sak.validering.PersontilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/saksoek")
@ProtectedWithClaims(issuer = "azuread")
class SakSøkController(
        private val sakSøkService: SakSøkService
) {

    @PostMapping("ident")
    fun søkMedIdent(@PersontilgangConstraint @RequestBody identParam: PersonIdentDto): Ressurs<SakSøkDto> {
        return sakSøkService.finnSakForPerson(identParam.personIdent)
    }

}
