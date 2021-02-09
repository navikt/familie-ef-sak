package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.BrevRequest
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.BrevService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(path = ["/api/brev"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtaksbrevController(private val brevService: BrevService,
                            private val tilgangService: TilgangService) {

    @PostMapping("/{behandlingId}")
    fun lagBrev(@PathVariable behandlingId: UUID, @RequestBody brevParams: BrevRequest): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val respons = brevService.lagBrev(behandlingId)

        return Ressurs.success(respons)
    }

    @GetMapping("/{behandlingId}")
    fun hentBrev(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val respons = brevService.hentBrev(behandlingId)

        return respons?.let { Ressurs.success(it.pdf) } ?: Ressurs.funksjonellFeil("Fant ingen brev for behandling")
    }
}