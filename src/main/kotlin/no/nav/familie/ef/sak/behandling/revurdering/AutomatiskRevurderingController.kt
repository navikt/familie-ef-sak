package no.nav.familie.ef.sak.behandling.revurdering

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/automatisk-revurdering"])
@ProtectedWithClaims(issuer = "azuread")
class AutomatiskRevurderingController(
    private val automatiskRevurderingService: AutomatiskRevurderingService,
    private val revurderingService: RevurderingService,
) {
    @PostMapping
    fun forsøkAutomatiskRevurdering(
        @RequestBody personIdenter: List<String>,
    ) {
        val identerForAutomatiskRevurdering =
            personIdenter.filter { personIdent ->
                automatiskRevurderingService.kanAutomatiskRevurderes(personIdent)
            }

        if (identerForAutomatiskRevurdering.isNotEmpty()) {
            revurderingService.opprettAutomatiskInntektsendringTask(identerForAutomatiskRevurdering)
        }
    }
}
