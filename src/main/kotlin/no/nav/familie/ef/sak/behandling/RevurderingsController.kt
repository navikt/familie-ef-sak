package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
    fun startRevurdering(@RequestBody revurderingInnhold: RevurderingDto): Ressurs<UUID> {
        if (!featureToggleService.isEnabled("familie.ef.sak.start-revurdering")) {
            throw Feil("Toggle for start revurdering er skrudd av",
                       "Kan ikke opprette revurdering da den er skrudd av")
        }
        tilgangService.validerTilgangTilFagsak(revurderingInnhold.fagsakId)
        tilgangService.validerHarSaksbehandlerrolle()
        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingInnhold)
        return Ressurs.success(revurdering.id)
    }


}