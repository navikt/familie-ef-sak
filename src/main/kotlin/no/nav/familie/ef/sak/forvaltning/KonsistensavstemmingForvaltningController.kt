package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.behandlingsflyt.task.KonsistensavstemmingPayload
import no.nav.familie.ef.sak.behandlingsflyt.task.KonsistensavstemmingTask
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/forvaltning/konsistensavstemming")
@ProtectedWithClaims(issuer = "azuread")
class KonsistensavstemmingForvaltningController(
    private val taskService: TaskService,
    private val tilgangService: TilgangService,
    private val iverksettClient: IverksettClient,
) {
    private val logger = Logg.getLogger(this::class)

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

    @GetMapping("test-timeout")
    fun timeoutTest(
        @RequestParam(name = "sekunder") sekunder: Long,
    ): String {
        tilgangService.validerHarForvalterrolle()
        return iverksettClient.timeoutTest(sekunder)
    }
}
