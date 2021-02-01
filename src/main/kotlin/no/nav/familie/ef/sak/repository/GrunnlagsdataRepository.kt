package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Grunnlagsdata
import no.nav.familie.ef.sak.repository.domain.SporbarUtils
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface GrunnlagsdataRepository : RepositoryInterface<Grunnlagsdata, UUID>, InsertUpdateRepository<Grunnlagsdata> {

    @Modifying
    @Query("UPDATE grunnlagsdata SET endret_tid=:endretTid")
    fun oppdaterEndretTid(@Param("endretTid") endretTid: LocalDateTime = SporbarUtils.now())
}
