package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.brev.dto.MellomlagreBrevRequestDto
import no.nav.familie.ef.sak.brev.dto.MellomlagretBrevResponse
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/brev/mellomlager/"])
@ProtectedWithClaims(issuer = "azuread")
class BrevMellomlagerController(
    private val tilgangService: TilgangService,
    private val mellomlagringBrevService: MellomlagringBrevService,
) {
    @PostMapping("/{behandlingId}")
    fun mellomlagreBrevverdier(
        @PathVariable behandlingId: UUID,
        @RequestBody mellomlagretBrev: MellomlagreBrevRequestDto,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return Ressurs.success(
            mellomlagringBrevService.mellomLagreBrev(
                behandlingId,
                mellomlagretBrev.brevverdier,
                mellomlagretBrev.brevmal,
                mellomlagretBrev.versjon,
            ),
        )
    }

    @GetMapping("/{behandlingId}")
    fun hentMellomlagretBrevverdier(
        @PathVariable behandlingId: UUID,
        @RequestParam sanityVersjon: String,
    ): Ressurs<MellomlagretBrevResponse?> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)

        return Ressurs.success(
            mellomlagringBrevService.hentOgValiderMellomlagretBrev(
                behandlingId,
                sanityVersjon,
            ),
        )
    }

    @PostMapping("/fagsak/{fagsakId}")
    fun mellomlagreFrittståendeSanitybrev(
        @PathVariable fagsakId: UUID,
        @RequestBody mellomlagreBrev: MellomlagreBrevRequestDto,
    ): Ressurs<UUID> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return Ressurs.success(
            mellomlagringBrevService.mellomLagreFrittståendeSanitybrev(
                fagsakId,
                mellomlagreBrev.brevverdier,
                mellomlagreBrev.brevmal,
            ),
        )
    }

    @GetMapping("/fagsak/{fagsakId}")
    fun hentMellomlagretFrittståendesanitybrev(
        @PathVariable fagsakId: UUID,
    ): Ressurs<MellomlagretBrevResponse?> {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)

        return Ressurs.success(mellomlagringBrevService.hentMellomlagretFrittståendeSanitybrev(fagsakId))
    }
}
