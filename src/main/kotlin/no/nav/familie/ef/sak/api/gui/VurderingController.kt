package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.VilkårVurderingDto
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.VurderingService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.ef.sak.validering.BehandlingConstraint
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
                          private val behandlingService: BehandlingService) {

    @PostMapping("inngangsvilkar")
    fun oppdaterVurderingInngangsvilkår(@RequestBody vilkårVurdering: VilkårVurderingDto): Ressurs<UUID> {
        return Ressurs.success(vurderingService.oppdaterVilkår(vilkårVurdering))
    }

    @GetMapping("{behandlingId}/inngangsvilkar")
    fun getInngangsvilkår(@BehandlingConstraint @PathVariable behandlingId: UUID): Ressurs<InngangsvilkårDto> {
        return Ressurs.success(vurderingService.hentInngangsvilkår(behandlingId))
    }

    @PostMapping("/{behandlingId}/inngangsvilkar/fullfor")
    fun validerInngangsvilkår(@BehandlingConstraint @PathVariable behandlingId: UUID): Ressurs<UUID> {
        val behandling = behandlingService.hentBehandling(behandlingId)
        return Ressurs.success(stegService.håndterInngangsvilkår(behandling).id)
    }
}
