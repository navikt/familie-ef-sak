package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Grunnlagsdata
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GrunnlagsdataRepository : RepositoryInterface<Grunnlagsdata, UUID>, InsertUpdateRepository<Grunnlagsdata> {

    // language=PostgreSQL
    @Query("""
        SELECT b.id
            FROM behandling b
            LEFT JOIN grunnlagsdata g ON b.id = g.behandling_id
            WHERE g.data IS NULL
            """)
    fun finnBehandlingerSomManglerGrunnlagsdata(): List<UUID>
}