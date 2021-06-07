package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository;
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TeknisktOpphørService(val behandlingService: BehandlingService, val behandlingRepository: BehandlingRepository, val vilkårsvurderingRepository: VilkårsvurderingRepository) {

    fun håndterTeknisktOpphør(personIdent: String) {

        val behandling = behandlingRepository.finnSisteBehandling(Stønadstype.OVERGANGSSTØNAD, setOf(personIdent))
        require(behandling != null) { throw Feil("Finner ikke behandling med stønadstype overgangsstønad for personen") }
        val tekniskOpphørBehandling = behandling.copy(id = UUID.randomUUID(),
                                       type = BehandlingType.TEKNISK_OPPHØR,
                                       status = BehandlingStatus.UTREDES)

        val nyBehandlingId = behandlingRepository.insert(tekniskOpphørBehandling).id

        val vilkår = vilkårsvurderingRepository.findByBehandlingId(behandling.id);

        // TODO: skal vi mappe over delvilkårsvurderinger og sette de til noe?
        val tekniskOpphørVilkår = vilkår.map {
            it.copy(id = nyBehandlingId,
                    resultat = Vilkårsresultat.SKAL_IKKE_VURDERES,
                    delvilkårsvurdering = DelvilkårsvurderingWrapper(emptyList()))
        }.forEach{vilkårsvurderingRepository.insert(it)}
    }
}