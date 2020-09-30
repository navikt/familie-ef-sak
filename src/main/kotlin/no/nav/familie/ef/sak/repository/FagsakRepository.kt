package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FagsakRepository : CrudRepository<Fagsak, UUID> {

    @Query("SELECT f.* FROM fagsak f LEFT JOIN fagsak_person fp ON fp.fagsak_id = f.id WHERE ident = :ident AND stonadstype = :stønadstype")
    fun findBySøkerIdent(ident: String, stønadstype: Stønadstype): Fagsak?

}
