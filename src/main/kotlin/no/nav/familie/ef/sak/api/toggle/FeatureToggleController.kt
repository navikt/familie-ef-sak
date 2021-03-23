package no.nav.familie.ef.sak.api.toggle

import no.nav.familie.ef.mottak.featuretoggle.FeatureToggleService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(path = ["/api/featuretoggle"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Unprotected
class FeatureToggleController(private val featureToggleService: FeatureToggleService) {

    val funksjonsbrytere = listOf("familie.ef.sak.journalfoer")

    @GetMapping
    fun sjekkAlle(): Map<String, Boolean> {
        return funksjonsbrytere.associateWith { featureToggleService.isEnabled(it) }
    }

    @GetMapping("/{toggleId}")
    fun sjekkFunksjonsbryter(@PathVariable toggleId: String,
                             @RequestParam("defaultverdi") defaultVerdi: Boolean? = false): Boolean {
        return featureToggleService.isEnabled(toggleId, defaultVerdi ?: false)
    }
}
