package no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsavtale
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SamværsavtaleRepository :
    RepositoryInterface<Samværsavtale, UUID>,
    InsertUpdateRepository<Samværsavtale> {
    fun findByBehandlingId(behandlingId: UUID): List<Samværsavtale>

    fun findByBehandlingIdAndBehandlingBarnId(
        behandlingId: UUID,
        behandlingBarnId: UUID,
    ): Samværsavtale?

    @Modifying
    @Query("DELETE from samvaersavtale where behandling_id = :behandlingId and behandling_barn_id = :behandlingBarnId")
    fun deleteByBehandlingIdAndBehandlingBarnId(
        behandlingId: UUID,
        behandlingBarnId: UUID,
    )
}
