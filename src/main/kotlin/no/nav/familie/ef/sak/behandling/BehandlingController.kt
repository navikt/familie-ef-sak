package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.infrastruktur.tilgang.TilgangService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(private val behandlingService: BehandlingService,
                           private val stegService: StegService,
                           private val tilgangService: TilgangService) {

    @GetMapping("{behandlingId}")
    fun hentBehandling(@PathVariable behandlingId: UUID): Ressurs<BehandlingDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling: Behandling = behandlingService.hentBehandling(behandlingId)
        val behandlingDto = behandling.tilDto()
        return Ressurs.success(behandlingDto)
    }

    @PostMapping("{behandlingId}/reset/{steg}")
    fun resetSteg(@PathVariable behandlingId: UUID, @PathVariable steg: StegType): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        stegService.resetSteg(behandlingId, steg)
        return Ressurs.success(behandlingId)
    }

    @PostMapping("{behandlingId}/annuller")
    fun annullerBehandling(@PathVariable behandlingId: UUID): Ressurs<BehandlingDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        tilgangService.validerHarSaksbehandlerrolle()
        val annullertBehandling = behandlingService.annullerBehandling(behandlingId)
        return Ressurs.success(annullertBehandling.tilDto())
    }

}