package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FagsakRepository : RepositoryInterface<Fagsak, UUID>, InsertUpdateRepository<Fagsak> {

    @Query("SELECT f.* FROM fagsak f LEFT JOIN fagsak_person fp ON fp.fagsak_id = f.id WHERE ident = :ident AND stonadstype = :stønadstype")
    fun findBySøkerIdent(ident: String, stønadstype: Stønadstype): Fagsak?

    @Query("SELECT f.*, fe.id AS eksternId_id " +
           "        FROM fagsak f " +
           "        JOIN fagsak_ekstern fe on fe.fagsak_id = f.id " +
           "        WHERE fe.id = :eksternId")
    fun finnMedEksternId(eksternId: Long): Fagsak?
}
