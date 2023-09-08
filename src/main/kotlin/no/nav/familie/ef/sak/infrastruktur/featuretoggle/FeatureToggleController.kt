package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import no.nav.familie.unleash.DefaultUnleashService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/featuretoggle"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Unprotected
class FeatureToggleController(
    private val featureToggleService: FeatureToggleService,
    val defaultUnleashService: DefaultUnleashService,
) {

    private val funksjonsbrytere = setOf(
        Toggle.BEHANDLING_KORRIGERING,
        Toggle.FRONTEND_VIS_IKKE_PUBLISERTE_BREVMALER,
        Toggle.OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST,
        Toggle.FRONTEND_AUTOMATISK_UTFYLLE_VILKÅR,
        Toggle.FRONTEND_SATSENDRING,
        Toggle.FRONTEND_VIS_INNTEKT_PERSONOVERSIKT,
    )

    private val unleashNextToggles = setOf(
        ToggleNext.TEST_USER_ID,
        ToggleNext.TEST_ENVIRONMENT,
        ToggleNext.MIGRERING_BARNETILSYN_NEXT,
    )

    @GetMapping
    fun sjekkAlle(): Map<String, Boolean> {
        val funksjonsbrytere = funksjonsbrytere.associate { it.toggleId to featureToggleService.isEnabled(it) }
        val funksjonsbrytereNext =
            unleashNextToggles.associate { it.toggleId to defaultUnleashService.isEnabled(it.toggleId) }

        val likeToggles = funksjonsbrytere.keys.intersect(funksjonsbrytereNext.keys)
        if (likeToggles.isNotEmpty()) {
            error("Like funksjonsbrytere funnet fra Unleash og Unleash Next: $likeToggles")
        }
        return funksjonsbrytere + funksjonsbrytereNext
    }

    @GetMapping("/{toggleId}")
    fun sjekkFunksjonsbryter(
        @PathVariable toggleId: String,
        @RequestParam("defaultverdi") defaultVerdi: Boolean? = false,
    ): Boolean {
        val toggle = byToggleId(toggleId)
        return when (toggle) {
            is ToggleWrapper.Funksjonsbrytere -> {
                featureToggleService.isEnabled(toggle.toggle, defaultVerdi ?: false)
            }

            is ToggleWrapper.UnleashNext -> {
                defaultUnleashService.isEnabled(toggle.toggleId, defaultVerdi ?: false)
            }
        }
    }
}
