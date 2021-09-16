package no.nav.familie.ef.sak.vedlegg

import no.nav.familie.ef.sak.infrastruktur.tilgang.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/vedlegg")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedleggController(private val vedleggService: VedleggService,
                        private val tilgangService: TilgangService) {

    @GetMapping("/{behandlingId}")
    fun finnVedleggForBehandling(@PathVariable behandlingId: UUID): Ressurs<JournalposterDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(vedleggService.finnJournalposter(behandlingId))
    }

}