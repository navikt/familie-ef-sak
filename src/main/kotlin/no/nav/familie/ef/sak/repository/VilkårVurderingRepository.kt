package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.VilkårVurdering
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårVurderingRepository: CrudRepository<VilkårVurdering, UUID> {

    fun findByBehandlingId(behandlingId: UUID): List<VilkårVurdering>
}