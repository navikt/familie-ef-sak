package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.OvergangsstønadDto
import no.nav.familie.ef.sak.service.OvergangsstøandService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

//Skal nok ikke bli brukt, men fin for dokumentasjon/swagger til en start
@RestController
@RequestMapping(path = ["/api/overgangsstonad"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OvergangsstøandController(private val overgangsstøandService: OvergangsstøandService) {

    @GetMapping("/{id}")
    fun dummy(@PathVariable("id") id: UUID): Ressurs<OvergangsstønadDto> {
        return Ressurs.failure("ikke tilgang")
        //return Ressurs.success(overgangsstøandService.lagOvergangsstønad(id))
    }

    @PostMapping("vurdering/aktivitetsplikt")
    fun vurderingAktivitetsplikt(@RequestBody sak: String): HttpStatus {
        return HttpStatus.CREATED
    }

    @PostMapping("vurdering/stillingsendring")
    fun vurderingStillingsendring(@RequestBody sak: String): HttpStatus {
        return HttpStatus.CREATED
    }

}
