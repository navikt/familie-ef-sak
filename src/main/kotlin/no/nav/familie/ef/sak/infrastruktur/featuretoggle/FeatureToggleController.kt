package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/featuretoggle"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class FeatureToggleController(
    private val featureToggleService: FeatureToggleService,
) {
    private val featureTogglesIBruk =
        setOf(
            Toggle.BEHANDLING_KORRIGERING,
            Toggle.FRONTEND_VIS_IKKE_PUBLISERTE_BREVMALER,
            Toggle.OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST,
            Toggle.FRONTEND_AUTOMATISK_UTFYLLE_VILKÅR,
            Toggle.FRONTEND_SATSENDRING,
            Toggle.HENLEGG_BEHANDLING_UTEN_OPPGAVE,
            Toggle.FRONTED_VIS_TILDEL_OPPGAVE_BEHANDLING,
            Toggle.VELG_ÅRSAK_VED_KLAGE_OPPRETTELSE,
            Toggle.FRONTEND_VIS_MARKERE_GODKJENNE_OPPGAVE_MODAL,
            Toggle.VIS_AUTOMATISK_INNTEKTSENDRING,
        )

    @GetMapping
    fun sjekkAlle(): Map<String, Boolean> = featureTogglesIBruk.associate { it.toggleId to featureToggleService.isEnabled(it) }

    @GetMapping("/{toggleId}")
    fun sjekkFunksjonsbryter(
        @PathVariable toggleId: String,
        @RequestParam("defaultverdi") defaultVerdi: Boolean? = false,
    ): Boolean {
        val toggle = Toggle.byToggleId(toggleId)
        return featureToggleService.isEnabled(toggle)
    }
}
