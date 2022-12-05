package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GrunnlagsdataRepository :
    RepositoryInterface<Grunnlagsdata, UUID>,
    InsertUpdateRepository<Grunnlagsdata> {

    @Modifying
    @Query("UPDATE grunnlagsdata SET data=:data WHERE behandling_id = :behandlingId")
    fun oppdaterData(behandlingId: UUID, data: GrunnlagsdataDomene): Int
}
