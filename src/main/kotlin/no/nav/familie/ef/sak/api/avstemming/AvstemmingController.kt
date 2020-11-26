package no.nav.familie.ef.sak.api.avstemming

import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/avstemming")
@ProtectedWithClaims(issuer = "azuread")
class AvstemmingController(private val avstemmingService: AvstemmingService) {

    @PostMapping("/grensesnittavstemming")
    fun opprettGrensesnittAvstemming(@RequestBody grensesnittavstemmingDto: GrensesnittavstemmingDto): Ressurs<Task?> {
        return Ressurs.success(avstemmingService.opprettGrensesnittavstemmingTask(grensesnittavstemmingDto))
    }

    @PostMapping("/konsistensavstemming")
    fun opprettKonsistensAvstemming(@RequestBody avstemmingDto: Array<KonsistensavstemmingDto>): Ressurs<List<Task>> {
        return Ressurs.success(avstemmingService.opprettKonsistenavstemmingTasker(*avstemmingDto))
    }
}