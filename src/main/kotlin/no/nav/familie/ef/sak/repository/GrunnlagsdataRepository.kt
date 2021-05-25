package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Grunnlagdata
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GrunnlagsdataRepository : RepositoryInterface<Grunnlagdata, UUID>, InsertUpdateRepository<Grunnlagdata> {
    // language=PostgreSQL

    @Query("""
        SELECT b.id
            FROM behandling b
                JOIN grunnlagsdata g ON b.id = g.behandling_id
            WHERE b.status IN (OPPRETTET, UTREDES) and g.data IS NULL
            """)
    fun finnBehandlingerSomManglerGrunnlagsdata(): List<UUID>
}