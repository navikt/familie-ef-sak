package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.BehandlingshistorikkDto
import no.nav.familie.ef.sak.repository.domain.tilDto
import no.nav.familie.ef.sak.service.BehandlingshistorikkService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(path = ["/api/behandlinghistorikk"])
@ProtectedWithClaims(issuer = "azuread")
class BehandlingshistorikkController(private val behandlingshistorikkService: BehandlingshistorikkService) {

    @GetMapping("{behandlingId}")
    fun hentBehandlingshistorikk(@PathVariable behandlingId: UUID): Ressurs<List<BehandlingshistorikkDto>> {
        val behandlingHistorikk = (behandlingshistorikkService.finnBehandlingshistorikk(behandlingId)).map { it.tilDto() }
        return Ressurs.success(behandlingHistorikk)
    }

}