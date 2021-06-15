package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.EksternId
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import org.springframework.data.jdbc.repository.query.Query
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
        SELECT b.*, be.id as eksternid_id
        FROM behandling b
        JOIN behandling_ekstern be ON b.id = be.behandling_id
        JOIN fagsak f ON f.id = b.fagsak_id
        JOIN fagsak_person fp ON b.fagsak_id = fp.fagsak_id
        WHERE fp.ident IN (:personidenter) AND f.stonadstype = :stønadstype AND b.type != 'BLANKETT'
        ORDER BY b.opprettet_tid DESC
        LIMIT 1
    """)
    fun finnSisteBehandlingSomIkkeErBlankett(stønadstype: Stønadstype, personidenter: Set<String>): Behandling?

    // language=PostgreSQL
    @Query("""
        SELECT b.*, be.id as eksternid_id
        FROM behandling b
        JOIN behandling_ekstern be ON b.id = be.behandling_id
        JOIN fagsak f ON f.id = b.fagsak_id
        JOIN fagsak_person fp ON b.fagsak_id = fp.fagsak_id
        WHERE fp.ident IN (:personidenter)
         AND f.stonadstype = :stønadstype
         AND b.type != 'BLANKETT'
         AND b.resultat != 'ANNULLERT'
         AND b.status = 'FERDIGSTILT'
        ORDER BY b.opprettet_tid DESC
        LIMIT 1
    """)
    fun finnSisteIverksatteBehandling(stønadstype: Stønadstype, personidenter: Set<String>): Behandling?

    // language=PostgreSQL
    @Query("""
        SELECT b.id
        FROM behandling b
        WHERE b.fagsak_id = :fagsakId
         AND b.type != 'BLANKETT'
         AND b.resultat != 'ANNULLERT'
         AND b.status = 'FERDIGSTILT'
        ORDER BY b.opprettet_tid DESC
        LIMIT 1
    """)
    fun finnSisteIverksatteBehandling(fagsakId: UUID): UUID?

    // language=PostgreSQL
    @Query("""
        SELECT b.id behandling_id, be.id ekstern_behandling_id, fe.id ekstern_fagsak_id
        FROM behandling b
            JOIN behandling_ekstern be ON b.id = be.behandling_id
            JOIN fagsak_ekstern fe ON b.fagsak_id = fe.fagsak_id
        """)
    fun finnEksterneIder(behandlingId: Set<UUID>): Set<EksternId>

    // language=PostgreSQL
    @Query("""
        SELECT id FROM (
            SELECT b.id, b.type, ROW_NUMBER() OVER (PARTITION BY b.fagsak_id ORDER BY b.opprettet_tid DESC) rn
            FROM behandling b
            JOIN fagsak f ON b.fagsak_id = f.id
                WHERE f.stonadstype = :stønadstype
                 AND b.status = 'FERDIGSTILT'
                 AND b.type != 'BLANKETT'
                 AND b.resultat != 'ANNULLERT'
         ) q WHERE rn = 1 AND type != 'TEKNISK_OPPHØR'
        """)
    fun finnSisteIverksatteBehandlingerSomIkkeErTekniskOpphør(stønadstype: Stønadstype): Set<UUID>

}
