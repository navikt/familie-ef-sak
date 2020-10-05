package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.VilkårVurdering
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårVurderingRepository: RepositoryInterface<VilkårVurdering, UUID>, InsertUpdateRepository<VilkårVurdering> {

    fun findByBehandlingId(behandlingId: UUID): List<VilkårVurdering>
}