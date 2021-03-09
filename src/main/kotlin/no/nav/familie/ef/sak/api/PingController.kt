package no.nav.familie.ef.sak.api

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Unprotected
@RestController
@RequestMapping("/api/ping")
class PingController {

    @GetMapping
    fun ping(): ResponseEntity<String> {
        return ResponseEntity.ok("pong")
    }
}
