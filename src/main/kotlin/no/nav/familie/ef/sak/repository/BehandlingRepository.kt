package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

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

    // language=PostgreSQL
    @Query("""
        SELECT EXISTS(SELECT b.id as eksternid_id
        FROM behandling b
        JOIN fagsak f ON f.id = b.fagsak_id
        JOIN fagsak_person fp ON b.fagsak_id = fp.fagsak_id
        WHERE fp.ident IN (:personidenter) AND f.stonadstype = :stonadstype AND b.type != 'BLANKETT'
        ORDER BY b.opprettet_tid DESC
        LIMIT 1)
    """)
    fun eksistererBehandlingSomIkkeErBlankett(@Param("stonadstype") stønadstype: Stønadstype, personidenter: Set<String>): Boolean

    // language=PostgreSQL
    @Query("""
        SELECT b.id
        FROM behandling b
        JOIN fagsak f ON f.id = b.fagsak_id
        JOIN behandling b2 ON b2.fagsak_id = f.id
        WHERE b.type != 'BLANKETT' AND b.resultat != 'ANNULLERT' AND b.status = 'FERDIGSTILT'
        AND b2.id = :behandlingId
        ORDER BY b.opprettet_tid DESC
        LIMIT 1
    """)
    fun finnSisteIverksatteBehandling(behandlingId: UUID): UUID?

}
