package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BrevRepository : RepositoryInterface<Vedtaksbrev, UUID>, InsertUpdateRepository<Vedtaksbrev> {

    // language=PostgreSQL
    @Query("""SELECT * FROM Brev
                     WHERE behandling = :behandlingId""")
    fun findByBehandlingId(behandlingId: UUID): Vedtaksbrev?

}
