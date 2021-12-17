package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GrunnlagsdataRepository : RepositoryInterface<Grunnlagsdata, UUID>, InsertUpdateRepository<Grunnlagsdata> {

    // language=PostgreSQL
    @Query("""
        SELECT b.id AS first, b.status AS second
            FROM behandling b
            LEFT JOIN grunnlagsdata g ON b.id = g.behandling_id
            WHERE g.data IS NULL
            """)
    fun finnBehandlingerSomManglerGrunnlagsdata(): List<Pair<UUID, String>>

}