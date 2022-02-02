package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.EksternId
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingRepository : RepositoryInterface<Behandling, UUID>, InsertUpdateRepository<Behandling> {

    fun findByFagsakId(fagsakId: UUID): List<Behandling>

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
                    JOIN fagsak_person_old fp ON b.fagsak_id=fp.fagsak_id
                    WHERE b.id = :behandlingId
                    ORDER BY fp.endret_tid DESC 
                    LIMIT 1
                    """)
    fun finnAktivIdent(behandlingId: UUID): String

    // language=PostgreSQL
    @Query("""
        SELECT b.*, be.id AS eksternid_id
        FROM behandling b
        JOIN behandling_ekstern be ON b.id = be.behandling_id
        JOIN fagsak f ON f.id = b.fagsak_id
        JOIN fagsak_person_old fp ON b.fagsak_id = fp.fagsak_id
        WHERE fp.ident IN (:personidenter) AND f.stonadstype = :stønadstype AND b.type != 'BLANKETT'
        ORDER BY b.opprettet_tid DESC
        LIMIT 1
    """)
    fun finnSisteBehandlingSomIkkeErBlankett(stønadstype: Stønadstype, personidenter: Set<String>): Behandling?

    // language=PostgreSQL
    @Query("""
        SELECT b.*, be.id AS eksternid_id
        FROM behandling b
        JOIN behandling_ekstern be ON b.id = be.behandling_id
        WHERE b.fagsak_id = :fagsakId
         AND b.type != 'BLANKETT'
         AND b.resultat IN ('OPPHØRT', 'INNVILGET')
         AND b.status = 'FERDIGSTILT'
        ORDER BY b.opprettet_tid DESC
        LIMIT 1
    """)
    fun finnSisteIverksatteBehandling(fagsakId: UUID): Behandling?

    // language=PostgreSQL
    @Query("""
        SELECT b.id behandling_id, be.id ekstern_behandling_id, fe.id ekstern_fagsak_id
        FROM behandling b
            JOIN behandling_ekstern be ON b.id = be.behandling_id
            JOIN fagsak_ekstern fe ON b.fagsak_id = fe.fagsak_id 
        WHERE behandling_id IN (:behandlingId)
        """)
    fun finnEksterneIder(behandlingId: Set<UUID>): Set<EksternId>

    // language=PostgreSQL
    @Query("""SELECT id FROM gjeldende_iverksatte_behandlinger WHERE stonadstype=:stønadstype""")
    fun finnSisteIverksatteBehandlinger(stønadstype: Stønadstype): Set<UUID>

}
