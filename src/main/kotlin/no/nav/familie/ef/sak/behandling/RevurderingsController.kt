package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
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
) {

    @PostMapping("{fagsakId}")
    fun startRevurdering(@RequestBody revurderingInnhold: RevurderingDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilFagsak(revurderingInnhold.fagsakId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()
        val revurdering = revurderingService.opprettRevurderingManuelt(revurderingInnhold)
        return Ressurs.success(revurdering.id)
    }

}