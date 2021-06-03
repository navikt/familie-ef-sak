package no.nav.familie.ef.sak.api.gui

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.VedtaksbrevService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(path = ["/api/brev"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VedtaksbrevController(private val brevService: VedtaksbrevService,
                            private val tilgangService: TilgangService) {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    @PostMapping("/{behandlingId}/{brevMal}")
    fun lagSaksbehandlerbrev(@PathVariable behandlingId: UUID,
                             @PathVariable brevMal: String,
                             @RequestBody brevRequest: JsonNode): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val data = brevService.lagSaksbehandlerBrev(behandlingId, brevRequest, brevMal)
        return Ressurs.success(data)
    }

    @GetMapping("/{behandlingId}")
    fun lagBeslutterbrev(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val beslutterPdf = brevService.lagBeslutterBrev(behandlingId).beslutterPdf
        return if (beslutterPdf != null) {
            Ressurs.success(beslutterPdf.bytes)
        } else {
            logger.error("Pdf finnes ikke for behandling=$behandlingId.")
            Ressurs.failure("Pdf for beslutter kunne ikke genereres")
        }
    }
}