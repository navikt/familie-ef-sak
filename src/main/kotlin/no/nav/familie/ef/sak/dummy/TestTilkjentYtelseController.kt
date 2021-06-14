package no.nav.familie.ef.sak.dummy

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.kontrakter.ef.iverksett.KonsistensavstemmingDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping(path = ["/api/test/"])
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class TestTilkjentYtelseController(private val testTilkjentYtelseService: TestTilkjentYtelseService,
                                   private val avstemmingService: AvstemmingService
) {

    @GetMapping("/konsistensavstemming/{stønadstype}")
    fun getKonsistensavstemming(@PathVariable stønadstype: Stønadstype): Ressurs<KonsistensavstemmingDto> {
        return Ressurs.success(testTilkjentYtelseService.konsistensavstemOppdrag(stønadstype))
    }

    @PostMapping("/konsistensavstemming/{stønadstype}")
    fun konsistensavstemming(@PathVariable stønadstype: Stønadstype): Ressurs<String> {
        avstemmingService.konsistensavstemOppdrag(stønadstype, LocalDate.now())
        return Ressurs.success("ok")
    }

}