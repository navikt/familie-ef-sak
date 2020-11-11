package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårsvurderingRepository : RepositoryInterface<Vilkårsvurdering, UUID>, InsertUpdateRepository<Vilkårsvurdering> {

    fun findByBehandlingId(behandlingId: UUID): List<Vilkårsvurdering>
}