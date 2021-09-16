package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/revurdering"])
@ProtectedWithClaims(issuer = "azuread")
class RevurderingsController(
        private val revurderingService: RevurderingService,
        private val tilgangService: TilgangService,
        private val featureToggleService: FeatureToggleService
) {

    @PostMapping("{fagsakId}")
    fun startRevurdering(@PathVariable fagsakId: UUID): Ressurs<UUID> {
        if (!featureToggleService.isEnabled("familie.ef.sak.start-revurdering")) {
            throw Feil("Toggle for start revurdering er skrudd av",
                       "Kan ikke opprette revurdering da den er skrudd av")
        }
        tilgangService.validerTilgangTilFagsak(fagsakId)
        val revurdering = revurderingService.opprettRevurderingManuelt(fagsakId)
        return Ressurs.success(revurdering.id)
    }


}