package no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.tilbakekreving.domain.tilDto
import no.nav.familie.ef.sak.tilbakekreving.dto.TilbakekrevingDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/tilbakekreving"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class TilbakekrevingController(private val tilgangService: TilgangService,
                               private val tilbakekrevingService: TilbakekrevingService) {

    @PostMapping("/{behandlingId}")
    fun lagreTilbakekreving(@PathVariable behandlingId: UUID, @RequestBody tilbakekrevingDto: TilbakekrevingDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        tilbakekrevingService.lagreTilbakekreving(tilbakekrevingDto, behandlingId)
        return Ressurs.success(behandlingId)
    }

    @GetMapping("/{behandlingId}")
    fun hentTilbakekreving(@PathVariable behandlingId: UUID): Ressurs<TilbakekrevingDto?> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(tilbakekrevingService.hentTilbakekreving(behandlingId)?.tilDto())
    }
}