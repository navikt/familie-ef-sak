package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.service.VurderingService
import no.nav.familie.ef.sak.util.RessursUtils.lagFrontendMelding
import org.springframework.stereotype.Service

@Service
class InngangsvilkårSteg(private val vurderingService: VurderingService) : BehandlingSteg<Void?> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.type == BehandlingType.TEKNISK_OPPHØR) return

        val vilkårSomManglerVurdering = vurderingService.hentInngangsvilkårSomManglerVurdering(behandling.id)

        if (vilkårSomManglerVurdering.isNotEmpty())
            throw Feil(frontendFeilmelding = lagFrontendMelding("Følgende inngangsvilkår mangler vurdering: ",
                                                                vilkårSomManglerVurdering.map { it.beskrivelse }),
                       message = "Validering av inngangsvilkår feilet for behandling ${behandling.id}")
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERE_INNGANGSVILKÅR
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {
        // gjør ikke noe
    }

}
