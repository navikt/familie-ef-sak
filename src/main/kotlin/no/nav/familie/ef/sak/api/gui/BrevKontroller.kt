package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.BrevRequest
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.BrevService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.boot.configurationprocessor.json.JSONObject
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(path = ["/api/brev"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BrevKontroller(private val brevService: BrevService,
                     private val behandlingService: BehandlingService,
                     private val tilgangService: TilgangService) {

    @PostMapping("/{behandlingId}")
    fun lagBrev(@PathVariable behandlingId: UUID, @RequestBody brevParams: BrevRequest): Ressurs<ByteArray> {
        val respons = brevService.lagBrev(behandlingId)

        return Ressurs.success(respons)
    }

    @GetMapping("/{behandlingId}")
    fun hentBrev(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        val respons = brevService.hentBrev(behandlingId)

        return respons?.let { Ressurs.success(it.pdf) } ?: Ressurs.funksjonellFeil("Fant ingen brev for behandling")
    }
}