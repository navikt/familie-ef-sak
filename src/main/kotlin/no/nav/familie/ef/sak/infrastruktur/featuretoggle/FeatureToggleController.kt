package no.nav.familie.ef.sak.infrastruktur.featuretoggle

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
class FeatureToggleController(private val featureToggleService: FeatureToggleService) {

    private val funksjonsbrytere = setOf(
        Toggle.BEHANDLING_KORRIGERING,
        Toggle.FRONTEND_VIS_IKKE_PUBLISERTE_BREVMALER,
        Toggle.MIGRERING,
        Toggle.MIGRERING_BARNETILSYN,
        Toggle.OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST,
        Toggle.FRONTEND_AUTOMATISK_UTFYLLE_VILKÅR,
        Toggle.FRONTEND_SATSENDRING,
        Toggle.FRONTEND_VIS_INNTEKT_PERSONOVERSIKT,
        Toggle.ULIKE_INNTEKTER,
        Toggle.VURDER_KONSEKVENS_OPPGAVER_LOKALKONTOR,
        Toggle.AUTOMATISKE_OPPGAVER_FREMLEGGSOPPGAVE,
        Toggle.UTBEDRET_GUI_SKOLEPENGER,
        Toggle.ÅRSAK_REVURDERING_BESKRIVELSE,
    )

    @GetMapping
    fun sjekkAlle(): Map<String, Boolean> {
        return funksjonsbrytere.associate { it.toggleId to featureToggleService.isEnabled(it) }
    }

    @GetMapping("/{toggleId}")
    fun sjekkFunksjonsbryter(
        @PathVariable toggleId: String,
        @RequestParam("defaultverdi") defaultVerdi: Boolean? = false,
    ): Boolean {
        val toggle = Toggle.byToggleId(toggleId)
        return featureToggleService.isEnabled(toggle, defaultVerdi ?: false)
    }
}
