package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.behandlingsflyt.task.KonsistensavstemmingPayload
import no.nav.familie.ef.sak.behandlingsflyt.task.KonsistensavstemmingTask
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
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
    private val featureToggleService: FeatureToggleService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun kjørKonsistensavstemming() {
        feilHvisIkke(erUtviklerMedVeilderrolle()) { "Kan kun kjøres av utvikler med veilederrolle" }
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

    private fun erUtviklerMedVeilderrolle(): Boolean =
        SikkerhetContext.erSaksbehandler() && featureToggleService.isEnabled(Toggle.UTVIKLER_MED_VEILEDERRROLLE)
}
