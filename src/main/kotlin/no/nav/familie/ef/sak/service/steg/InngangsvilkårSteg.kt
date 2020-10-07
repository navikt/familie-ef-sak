package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.service.VurderingService
import no.nav.familie.ef.sak.util.RessursUtils.lagFrontendMelding
import org.springframework.stereotype.Service

@Service
class InngangsvilkårSteg(private val vurderingService: VurderingService): BehandlingSteg<String> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: String): StegType {
        //TODO skal vi gjøre noe her?
        return hentNesteSteg(behandling)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERE_INNGANGSVILKÅR
    }

    override fun postValiderSteg(behandling: Behandling) {
        if (behandling.type == BehandlingType.TEKNISK_OPPHØR) return

        val vilkårSomManglerVurdering = vurderingService.hentInngangsvilkårSomManglerVurdering(behandling.id)

        if (vilkårSomManglerVurdering.isNotEmpty())
            throw Feil(frontendFeilmelding = lagFrontendMelding("Følgende inngangsvilkår mangler vurdering: ",
                                                                vilkårSomManglerVurdering.map { it.beskrivelse }),
                       message = "Validering av vilkårsvurdering feilet for behandling ${behandling.id}")
    }
}