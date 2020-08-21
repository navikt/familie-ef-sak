package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.SakDto
import no.nav.familie.ef.sak.service.SakService
import no.nav.familie.ef.sak.validering.SakstilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/sak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SakController(private val sakService: SakService) {

    @GetMapping("/{id}")
    fun hentSak(@SakstilgangConstraint @PathVariable("id") id: UUID): Ressurs<SakDto> {
        return Ressurs.success(sakService.hentOvergangsstønadDto(id))
    }

}
