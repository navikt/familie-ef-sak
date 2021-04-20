package no.nav.familie.ef.sak.api.iverksettdummy

import no.nav.familie.ef.sak.service.IverksettService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/iverksett"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class IverksettController(val iverksettService: IverksettService) {

    @GetMapping("/test")
    fun dummy(): ResponseEntity<String> {
        return ResponseEntity<String>(iverksettService.test(), HttpStatus.OK)
    }

}