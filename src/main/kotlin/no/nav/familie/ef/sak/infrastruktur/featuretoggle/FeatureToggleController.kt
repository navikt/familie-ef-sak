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

    private val funksjonsbrytere = listOf("familie.ef.sak.tekniskopphor",
                                          "familie.ef.sak.frontend-vis-ikke-publiserte-brevmaler",
                                          "familie.ef.sak.frontend-vis-oppdatering-av-registeropplysninger",
                                          "familie.ef.sak.brevmottakere-verge-og-fullmakt",
                                          "familie.ef.sak.migrering",
                                          "familie.ef.sak.frontend-vis-sanksjon-en-maned",
                                          "familie.ef.sak.kan-legge-til-nye-barn-paa-revurdering",
                                          "familie.ef.sak.frontend-vis-tilbakekreving",
                                          "familie.ef.sak.frontend-oppgavebenk-migrer-fagsak"
    )

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
