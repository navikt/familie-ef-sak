package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Regelendring2026
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface Regelendring2026Repository :
    RepositoryInterface<Regelendring2026, UUID>,
    InsertUpdateRepository<Regelendring2026> {
    fun findByBehandlingId(behandlingId: UUID): Regelendring2026?

    @Modifying
    @Query(
        """
        INSERT INTO regelendring_2026 (behandling_id, begrunnelse)
        VALUES (:behandlingId, :begrunnelse)
        ON CONFLICT (behandling_id) DO UPDATE SET
            begrunnelse = excluded.begrunnelse
        """,
    )
    fun upsert(
        behandlingId: UUID,
        begrunnelse: String,
    )
}
