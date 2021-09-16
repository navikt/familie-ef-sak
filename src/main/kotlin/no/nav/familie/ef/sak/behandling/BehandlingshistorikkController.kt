package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.api.dto.BehandlingshistorikkDto
import no.nav.familie.ef.sak.behandling.domain.tilDto
import no.nav.familie.ef.sak.behandling.BehandlingshistorikkService
import no.nav.familie.ef.sak.infrastruktur.tilgang.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(path = ["/api/behandlingshistorikk"])
@ProtectedWithClaims(issuer = "azuread")
class BehandlingshistorikkController(private val behandlingshistorikkService: BehandlingshistorikkService,
                                     private val tilgangService: TilgangService) {

    @GetMapping("{behandlingId}")
    fun hentBehandlingshistorikk(@PathVariable behandlingId: UUID): Ressurs<List<BehandlingshistorikkDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandlingHistorikk = (behandlingshistorikkService.finnBehandlingshistorikk(behandlingId)).map { it.tilDto() }
        return Ressurs.success(behandlingHistorikk)
    }

}