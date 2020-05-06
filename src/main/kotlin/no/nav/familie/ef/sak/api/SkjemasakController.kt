package no.nav.familie.ef.sak.api

import no.nav.familie.ba.sak.validering.SakstilgangConstraint
import no.nav.familie.ef.sak.service.SakService
import no.nav.familie.kontrakter.ef.sak.Sak
import no.nav.familie.kontrakter.ef.sak.Skjemasak
import no.nav.familie.kontrakter.ef.søknad.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.Month
import java.util.*

@RestController
@RequestMapping(path = ["/api/skjemasak"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SkjemasakController(private val sakService: SakService) {

    @PostMapping("sendInn")
    fun sendInn(@RequestBody skjemasak: Skjemasak): HttpStatus {
        return HttpStatus.OK
    }
}

