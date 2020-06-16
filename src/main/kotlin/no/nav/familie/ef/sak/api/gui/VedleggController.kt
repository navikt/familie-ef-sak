package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.service.VedleggService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*


@RestController
@RequestMapping(path = ["/api/vedlegg"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedleggController(val vedleggService: VedleggService) {

    @GetMapping("{id}")
    fun hentVedlegg(@PathVariable id: UUID): ResponseEntity<ByteArray> {
        val vedlegg = vedleggService.hentVedlegg(id)
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=${vedlegg.navn}")
                .body(vedlegg.data)
    }
}
