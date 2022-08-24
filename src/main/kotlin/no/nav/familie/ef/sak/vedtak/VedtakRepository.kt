package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VedtakRepository : RepositoryInterface<Vedtak, UUID>, InsertUpdateRepository<Vedtak> {

    @Query("SELECT behandling_id from vedtak")
    fun finnAlleIder(): List<UUID>
}
