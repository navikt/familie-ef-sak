package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface VilkårsvurderingRepository : RepositoryInterface<Vilkårsvurdering, UUID>, InsertUpdateRepository<Vilkårsvurdering> {

    fun findByBehandlingId(behandlingId: UUID): List<Vilkårsvurdering>

    @Modifying
    @Query("DELETE from vilkarsvurdering where behandling_id = :behandlingId")
    fun deleteByBehandlingId(behandlingId: UUID)

    @Modifying
    @Query("UPDATE vilkarsvurdering SET endret_tid = :nyttTidspunkt WHERE id = :id")
    fun oppdaterEndretTid(id: UUID, nyttTidspunkt: LocalDateTime)

    @Modifying
    @Query("UPDATE vilkarsvurdering SET opprettet_av = 'VL', endret_av = 'VL' WHERE id = :id")
    fun settMaskinelltOpprettet(id: UUID)

    fun findByTypeAndBehandlingIdIn(vilkårtype: VilkårType, behandlingIds: Collection<UUID>): List<Vilkårsvurdering>
}
