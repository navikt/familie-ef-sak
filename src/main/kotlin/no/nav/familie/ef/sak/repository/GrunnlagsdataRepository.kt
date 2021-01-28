package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Grunnlagsdata
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GrunnlagsdataRepository : RepositoryInterface<Grunnlagsdata, UUID>, InsertUpdateRepository<Grunnlagsdata> {

    @Query("SELECT diff FROM grunnlagsdata WHERE behandling_id=:behandling_id")
    fun harDiffIGrunnlagsdata(@Param("behandling_id") behandlingId: UUID): Boolean?
}
