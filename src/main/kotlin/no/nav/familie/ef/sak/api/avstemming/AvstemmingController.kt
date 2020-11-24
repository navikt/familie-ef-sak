package no.nav.familie.ef.sak.api.avstemming

import no.nav.familie.ef.sak.repository.KonsistensavstemmingRepository
import no.nav.familie.ef.sak.repository.domain.Konsistensavstemming
import no.nav.familie.ef.sak.service.AvstemmingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.domene.Task
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/avstemming")
@ProtectedWithClaims(issuer = "azuread")
class AvstemmingController(
        private val avstemmingService: AvstemmingService,
        private val konsistensavstemmingRepository: KonsistensavstemmingRepository,
) {

    @PostMapping
    fun utløsGrensesnittavstemming(@RequestBody avstemmingDto: AvstemmingDto): Ressurs<Task?> {
        return Ressurs.success(avstemmingService.utløsAvstemming(avstemmingDto))
    }

    @PostMapping("/datoerforavstemming")
    fun utløsGrensesnittavstemming(@RequestBody avstemmingDto: List<KonsistenavstemmingDto>): Ressurs<List<Konsistensavstemming>> {
        return avstemmingDto
                .map { Konsistensavstemming(stønadstype = it.stønadstype, dato = it.datoForAvstemming) }
                .let { konsistensavstemmingRepository.insertAll(it) }
                .let { Ressurs.success(it)}

    }
}