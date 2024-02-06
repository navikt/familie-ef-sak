package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.behandlingsflyt.task.KonsistensavstemmingPayload
import no.nav.familie.ef.sak.behandlingsflyt.task.KonsistensavstemmingTask
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/forvaltning/konsistensavstemming")
@ProtectedWithClaims(issuer = "azuread")
class KonsistensavstemmingForvaltningController(
    private val taskService: TaskService,
    private val tilgangService: TilgangService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun kjørKonsistensavstemming() {
        tilgangService.validerHarForvalterrolle()
        val triggerdato = LocalDate.now()
        logger.info("Oppretter manuell tasks for konsistensavstemming for dato=$triggerdato")
        taskService.saveAll(
            listOf(
                KonsistensavstemmingTask.opprettTask(
                    KonsistensavstemmingPayload(StønadType.OVERGANGSSTØNAD, triggerdato),
                    triggerdato.atTime(22, 0),
                ),
                KonsistensavstemmingTask.opprettTask(
                    KonsistensavstemmingPayload(StønadType.BARNETILSYN, triggerdato),
                    triggerdato.atTime(22, 20),
                ),
            ),
        )
    }
}
