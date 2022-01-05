package no.nav.familie.ef.sak.behandlingshistorikk

import no.nav.familie.ef.sak.behandlingshistorikk.dto.HendelseshistorikkDto
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/behandlingshistorikk"])
@ProtectedWithClaims(issuer = "azuread")
class BehandlingshistorikkController(private val behandlingshistorikkService: BehandlingshistorikkService,
                                     private val tilgangService: TilgangService) {

    @GetMapping("{behandlingId}")
    fun hentBehandlingshistorikk(@PathVariable behandlingId: UUID): Ressurs<List<HendelseshistorikkDto>> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandlingHistorikk = behandlingshistorikkService.finnHendelseshistorikk(behandlingId)
        return Ressurs.success(behandlingHistorikk)
    }
}