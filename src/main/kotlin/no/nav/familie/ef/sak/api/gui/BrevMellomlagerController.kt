package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.MellomlagretBrevDto
import no.nav.familie.ef.sak.repository.domain.MellomlagretBrev
import no.nav.familie.ef.sak.service.MellomlagringBrevService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.util.RessursUtils
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
class BrevMellomlagerController(private val tilgangService: TilgangService,
                                private val mellomlagringBrevService: MellomlagringBrevService) {

    @PostMapping("/{behandlingId}")
    fun mellomlagreBrevverdier(@PathVariable behandlingId: UUID,
                               @RequestBody mellomlagretBrev: MellomlagretBrevDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)

        return Ressurs.success(mellomlagringBrevService.mellomLagreBrev (behandlingId,
                                                 mellomlagretBrev.brevverdier,
                                                 mellomlagretBrev.brevmal,
                                                 mellomlagretBrev.versjon))
    }


    @GetMapping("/{behandlingId}")
    fun hentMellomlagretBrevverdier(@PathVariable behandlingId: UUID,
                                    @RequestParam brevmal: String,
                                    @RequestParam sanityVersjon: String): Ressurs<String?> {
        tilgangService.validerTilgangTilBehandling(behandlingId)

        return Ressurs.success(mellomlagringBrevService.hentOgValiderMellomlagretBrev(behandlingId,
                                                                                      brevmal,
                                                                                      sanityVersjon)?.brevverdier)
    }

}
