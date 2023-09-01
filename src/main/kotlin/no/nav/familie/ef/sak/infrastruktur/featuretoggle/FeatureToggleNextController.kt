package no.nav.familie.ef.sak.infrastruktur.featuretoggle

import no.nav.familie.unleash.DefaultUnleashService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/featuretogglenext"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class FeatureToggleNextController(
    @Value("\${UNLEASH_SERVER_API_URL}") private val apiUrl: String,
    @Value("\${UNLEASH_SERVER_API_TOKEN}") private val apiToken: String,
    @Value("\${NAIS_APP_NAME}") private val appName: String,
) {

    private val defaultUnleashService: DefaultUnleashService = DefaultUnleashService(apiUrl, apiToken, appName)

    private val unleashNextToggles = setOf(
        Toggle.TEST_USER_ID,
        Toggle.TEST_ENVIRONMENT,
    )

    @GetMapping
    fun sjekkAlle(): Map<String, Boolean> {
        return unleashNextToggles.associate { it.toggleId to defaultUnleashService.isEnabled(it.toggleId) }
    }

    @GetMapping("/{toggleId}")
    fun sjekkFunksjonsbryter(
        @PathVariable toggleId: String,
        @RequestParam("defaultverdi") defaultVerdi: Boolean? = false,
    ): Boolean {
        val toggle = Toggle.byToggleId(toggleId)
        return defaultUnleashService.isEnabled(toggle.toggleId, defaultVerdi ?: false)
    }
}
