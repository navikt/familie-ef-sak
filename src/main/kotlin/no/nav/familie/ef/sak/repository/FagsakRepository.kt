package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Fagsak
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FagsakRepository : CrudRepository<Fagsak, UUID> {

    fun findBySøkerIdent(ident: String): Fagsak?

}
