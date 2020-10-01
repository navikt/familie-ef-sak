package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.Aleneomsorg
import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto
import no.nav.familie.ef.sak.service.VurderingService
import no.nav.familie.ef.sak.validering.SakstilgangConstraint
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/vurdering"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VurderingController(val vurderingService: VurderingService) {

    @GetMapping("{sakId}/inngangsvilkar")
    fun getInngangsvilkår(@SakstilgangConstraint @PathVariable sakId: UUID): Ressurs<InngangsvilkårDto> {
        return Ressurs.success(vurderingService.hentInngangsvilkår(sakId))
    }

    @GetMapping("{sakId}/aleneomsorg")
    fun getAleneOmsorg(@SakstilgangConstraint @PathVariable sakId: UUID): Ressurs<Aleneomsorg> {
        return Ressurs.success(vurderingService.vurderAleneomsorg(sakId))
    }

}
