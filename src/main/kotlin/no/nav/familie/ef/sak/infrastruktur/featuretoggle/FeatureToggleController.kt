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
            FeatureToggle.BehandlingKorrigering,
            FeatureToggle.FrontendVisIkkePubliserteBrevmaler,
            FeatureToggle.OpprettBehandlingFerdigstiltJournalpost,
            FeatureToggle.FrontendAutomatiskUtfylleVilkar,
            FeatureToggle.FrontendSatsendring,
            FeatureToggle.FrontendKonverterDelmalblokkTilHtmlFelt,
            FeatureToggle.HenleggBehandlingUtenOppgave,
            FeatureToggle.FrontendVisTildelOppgaveBehandling,
            FeatureToggle.VelgÅrsakVedKlageOpprettelse,
            FeatureToggle.FrontendVisMarkereGodkjenneOppgaveModal,
            FeatureToggle.VisSamværskalkulator,
            FeatureToggle.VisAutomatiskInntektsendring,
        )

    @GetMapping
    fun sjekkAlle(): Map<String, Boolean> = featureTogglesIBruk.associate { it.toggleId to featureToggleService.isEnabled(it) }

    @GetMapping("/{toggleId}")
    fun sjekkFunksjonsbryter(
        @PathVariable toggleId: String,
        @RequestParam("defaultverdi") defaultVerdi: Boolean? = false,
    ): Boolean {
        val featureToggle = FeatureToggle.fraToggleId(toggleId)
        return featureToggleService.isEnabled(featureToggle)
    }
}
