package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.VurderingService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping(path = ["/api/vurdering"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VurderingController(private val vurderingService: VurderingService,
                          private val stegService: StegService,
                          private val behandlingService: BehandlingService,
                          private val tilgangService: TilgangService) {

    @PostMapping("inngangsvilkar")
    fun oppdaterVurderingInngangsvilkår(@RequestBody vilkårsvurdering: VilkårsvurderingDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(vilkårsvurdering.behandlingId)
        return Ressurs.success(vurderingService.oppdaterVilkår(vilkårsvurdering))
    }

    @GetMapping("{behandlingId}/inngangsvilkar")
    fun getInngangsvilkår(@PathVariable behandlingId: Long): Ressurs<InngangsvilkårDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        return Ressurs.success(vurderingService.hentInngangsvilkår(behandling.id))
    }

    @PostMapping("/{behandlingId}/inngangsvilkar/fullfor")
    fun validerInngangsvilkår(@PathVariable behandlingId: Long): Ressurs<Long> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        return Ressurs.success(stegService.håndterInngangsvilkår(behandling).eksternId.id)
    }
}
