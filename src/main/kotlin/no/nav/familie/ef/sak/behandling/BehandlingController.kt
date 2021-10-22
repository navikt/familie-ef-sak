package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.HenlagtÅrsak.BEHANDLES_I_GOSYS
import no.nav.familie.ef.sak.behandling.HenlagtÅrsak.FEILREGISTRERT
import no.nav.familie.ef.sak.behandling.HenlagtÅrsak.TRUKKET_TILBAKE
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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

    @PostMapping("{behandlingId}/henlegg")
    fun henleggBehandling(@PathVariable behandlingId: UUID, @RequestBody henlagt: HenlagtDto): Ressurs<BehandlingDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        tilgangService.validerHarSaksbehandlerrolle()
        validerÅrsakPAsserBehandlingstype(henlagt.årsak, behandlingService.hentBehandling(behandlingId))
        val henlagtBehandling = behandlingService.henleggBehandling(behandlingId, henlagt)
        return Ressurs.success(henlagtBehandling.tilDto())
    }

    private fun validerÅrsakPAsserBehandlingstype(årsak: HenlagtÅrsak, hentBehandling: Behandling) {
        when (årsak) {
            BEHANDLES_I_GOSYS -> feilHvis(hentBehandling.type !== BehandlingType.BLANKETT) { "Bare blankett kan henlegges med årsak BEHANDLES_I_GOSYS" }
            FEILREGISTRERT -> feilHvis(hentBehandling.type == BehandlingType.BLANKETT) { "Blankett kan bare henlegges med årsak BEHANDLES_I_GOSYS" }
            TRUKKET_TILBAKE -> feilHvis(hentBehandling.type == BehandlingType.BLANKETT) { "Blankett kan bare henlegges med årsak BEHANDLES_I_GOSYS" }
        }
    }
}

data class HenlagtDto(val årsak: HenlagtÅrsak)

enum class HenlagtÅrsak {
    TRUKKET_TILBAKE,
    FEILREGISTRERT,
    BEHANDLES_I_GOSYS
}
