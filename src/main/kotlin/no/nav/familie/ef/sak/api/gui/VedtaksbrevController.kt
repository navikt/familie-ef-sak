package no.nav.familie.ef.sak.api.gui

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.service.VedtaksbrevService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import springfox.documentation.spring.web.json.Json
import java.util.*

@RestController
@RequestMapping(path = ["/api/brev"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtaksbrevController(private val brevService: VedtaksbrevService,
                            private val tilgangService: TilgangService) {

    @PostMapping("/{behandlingId}")
    fun forhåndsvisBrev(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val respons = brevService.forhåndsvisBrev(behandlingId)

        return Ressurs.success(respons)
    }

    @PostMapping("/{behandlingId}/{brevMal}")
    fun forhåndsvisBrevV2(@PathVariable behandlingId: UUID, @PathVariable brevMal: String, @RequestBody utfylltBrev: String): Ressurs<ByteArray> {
        //tilgangService.validerTilgangTilBehandling(behandlingId) Todo
        val respons = brevService.forhåndsvisBrev(behandlingId, objectMapper.readTree(utfylltBrev), brevMal)

        return Ressurs.success(respons)
    }


    @GetMapping("/{behandlingId}")
    fun hentBrev(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return  Ressurs.success(brevService.hentBrev(behandlingId).utkastPdf.bytes)
    }
}