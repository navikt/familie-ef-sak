package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.VurderingDto
import no.nav.familie.ef.sak.service.VurderingService
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
class VurderingController(val vurderingService: VurderingService) {

    @PostMapping("inngangsvilkar")
    fun oppdaterVurderingInngangsvilkår(@RequestBody vurdering: VurderingDto): Ressurs<UUID> {
        return Ressurs.success(vurderingService.oppdaterVilkår(vurdering))
    }

    @GetMapping("{behandlingId}/inngangsvilkar")
    fun getInngangsvilkår(@BehandlingConstraint @PathVariable behandlingId: UUID): Ressurs<InngangsvilkårDto> {
        return Ressurs.success(vurderingService.hentInngangsvilkår(behandlingId))
    }

}
