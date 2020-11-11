package no.nav.familie.ef.sak.api.avstemming

import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.task.GrensesnittavstemmingTask
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/grensesnittavstemming")
@ProtectedWithClaims(issuer = "azuread")
class AvstemmingController (private val grensesnittavstemmingTask: GrensesnittavstemmingTask) {

    @PostMapping("{stønadstype}")
    fun utløsGrensesnittavstemming(@PathVariable stønadstype: Stønadstype, @RequestBody grensesnittavstemmingDto: GrensesnittavstemmingDto): Ressurs<Task> {
        return Ressurs.success(grensesnittavstemmingTask.utløsGrensesnittavstemming(fraDato = grensesnittavstemmingDto.fraDato, stønadstype = stønadstype, triggerTid = grensesnittavstemmingDto.triggerTid))
    }

}