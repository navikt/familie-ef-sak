package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.BehandlingsHistorikkDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.repository.domain.tilDto
import no.nav.familie.ef.sak.service.BehandlingHistorikkService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(path = ["/api/behandlinghistorikk"])
@ProtectedWithClaims(issuer = "azuread")
class BehandlingsHistorikkController(private val behandlingHistorikkService: BehandlingHistorikkService) {

    @GetMapping("{behandlingId}")
    fun hentTilkjentYtelse(@PathVariable behandlingId: UUID): Ressurs<List<BehandlingsHistorikkDto>> {
        val behandlingHistorikk = (behandlingHistorikkService.finnBehandlingHistorikk(behandlingId)).map { it.tilDto() }
        return Ressurs.success(behandlingHistorikk)
    }

}