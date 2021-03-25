package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingRepository : RepositoryInterface<Behandling, UUID>, InsertUpdateRepository<Behandling> {

    fun findByFagsakId(fagsakId: UUID): List<Behandling>

    fun findByFagsakIdAndAktivIsTrue(fagsakId: UUID): Behandling?

    fun findByFagsakIdAndStatus(fagsakId: UUID, status: BehandlingStatus): List<Behandling>

    // language=PostgreSQL
    @Query("""SELECT b.*, be.id AS eksternid_id         
                     FROM behandling b         
                     JOIN behandling_ekstern be 
                     ON be.behandling_id = b.id         
                     WHERE be.id = :eksternId""")
    fun finnMedEksternId(eksternId: Long): Behandling?

    // language=PostgreSQL
    @Query("""SELECT fp.ident FROM fagsak f
                    JOIN behandling b ON f.id = b.fagsak_id
                    JOIN fagsak_person fp ON b.fagsak_id=fp.fagsak_id
                    WHERE b.id = :behandlingId
                    ORDER BY fp.opprettet_tid DESC 
                    LIMIT 1
                    """)
    fun finnAktivIdent(behandlingId: UUID): String

}
