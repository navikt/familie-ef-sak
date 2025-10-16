package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
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
            Toggle.FRONTEND_KOPIER_KNAPP_ERROR_ALERT,
            Toggle.OPPRETT_BEHANDLING_FERDIGSTILT_JOURNALPOST,
            Toggle.FRONTEND_AUTOMATISK_UTFYLLE_VILKÅR,
            Toggle.FRONTEND_SATSENDRING,
            Toggle.HENLEGG_BEHANDLING_UTEN_OPPGAVE,
            Toggle.FRONTED_VIS_TILDEL_OPPGAVE_BEHANDLING,
            Toggle.VELG_ÅRSAK_VED_KLAGE_OPPRETTELSE,
            Toggle.VIS_AUTOMATISK_INNTEKTSENDRING,
            Toggle.BEHANDLE_AUTOMATISK_INNTEKTSENDRING,
            Toggle.OPPDATER_BEHANDLINGSTATUS,
            Toggle.FRONTEND_VIS_BEREGNINGSSKJEMA,
            Toggle.VIS_ANDRE_YTELSER,
            Toggle.MIGRERING_BARNETILSYN,
        )

    @GetMapping
    fun sjekkAlle(): Map<String, Boolean> = featureTogglesIBruk.associate { it.toggleId to featureToggleService.isEnabled(it) }

    @GetMapping("/{toggleId}")
    fun sjekkFunksjonsbryter(
        @PathVariable toggleId: String,
    ): Boolean {
        val toggle = Toggle.byToggleId(toggleId)
        return featureToggleService.isEnabled(toggle)
    }
}
