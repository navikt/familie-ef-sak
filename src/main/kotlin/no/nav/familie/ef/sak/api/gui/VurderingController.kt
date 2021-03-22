package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.VilkårDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.VurderingService
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
class VurderingController(
    private val vurderingService: VurderingService,
    private val tilgangService: TilgangService
) {

    @PostMapping(value = ["inngangsvilkar", "vilkar"])
    fun oppdaterVurderingVilkår(@RequestBody vilkårsvurdering: VilkårsvurderingDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(vilkårsvurdering.behandlingId)
        vurderingService.oppdaterVilkår(vilkårsvurdering)
        vurderingService.oppdaterStegPåBehandling(vilkårsvurdering.behandlingId)
        return Ressurs.success(vilkårsvurdering.id)
    }

    @GetMapping(value = ["{behandlingId}/inngangsvilkar", "{behandlingId}/vilkar"])
    fun getVilkår(@PathVariable behandlingId: UUID): Ressurs<VilkårDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(vurderingService.hentVilkår(behandlingId))
    }
}
