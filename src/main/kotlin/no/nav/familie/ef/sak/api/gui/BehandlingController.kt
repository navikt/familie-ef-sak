package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.BehandlingDto
import no.nav.familie.ef.sak.api.dto.tilDto
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.GrunnlagsdataService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingController(private val behandlingService: BehandlingService,
                           private val stegService: StegService,
                           private val grunnlagsdataService: GrunnlagsdataService,
                           private val tilgangService: TilgangService) {

    @GetMapping("{behandlingId}")
    fun hentBehandling(@PathVariable behandlingId: UUID): Ressurs<BehandlingDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val behandlingDto = if (!behandling.status.behandlingErLåstForVidereRedigering()) {
            behandling.tilDto(grunnlagsdiff = grunnlagsdataService.harDiffIGrunnlagsdata(behandling))
        } else {
            behandling.tilDto()
        }
        return Ressurs.success(behandlingDto)
    }

    @PostMapping("{behandlingId}/reset/{steg}")
    fun oppdaterGrunnlagsdata(@PathVariable behandlingId: UUID, @PathVariable steg: StegType): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        stegService.resetSteg(behandlingId, steg)
        return Ressurs.success(behandlingId)
    }

}