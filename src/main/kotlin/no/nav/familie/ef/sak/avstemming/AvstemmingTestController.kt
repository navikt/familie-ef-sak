package no.nav.familie.ef.sak.avstemming

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/konsistensavstemming")
@ProtectedWithClaims(issuer = "azuread")
@Profile("dev", "local")
class AvstemmingTestController(private val avstemmingService: AvstemmingService) {

    @PostMapping
    fun utførKonsistensavstemming(@RequestParam("type") stønadstype: Stønadstype): Ressurs<String> {
        avstemmingService.konsistensavstemOppdrag(stønadstype, LocalDate.now())
        return Ressurs.success("OK")
    }
}