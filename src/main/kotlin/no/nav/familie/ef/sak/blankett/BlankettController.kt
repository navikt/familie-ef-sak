package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.VurderingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/blankett"])
@ProtectedWithClaims(issuer = "azuread")
class BlankettController(private val tilgangService: TilgangService,
                         private val vurderingService: VurderingService,
                         private val blankettClient: BlankettClient) {

    @GetMapping("{behandlingId}")
    fun lagBlankettPdf(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val inngangsvilkår = vurderingService.hentInngangsvilkår(behandlingId)
        return Ressurs.success(blankettClient.genererBlankett(inngangsvilkår))
    }
}