package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.fagsak.domain.FagsakDomain
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FagsakRepository : RepositoryInterface<FagsakDomain, UUID>, InsertUpdateRepository<FagsakDomain> {

    // language=PostgreSQL
    @Query("""SELECT DISTINCT f.*, fe.id AS eksternid_id
                    FROM fagsak f 
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
                    LEFT JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id 
                    WHERE ident IN (:personIdenter)
                    AND stonadstype = :stønadstype""")
    fun findBySøkerIdent(personIdenter: Set<String>, stønadstype: Stønadstype): FagsakDomain?

    fun findByFagsakPersonIdAndStønadstype(fagsakPersonId: UUID, stønadstype: Stønadstype): FagsakDomain?

    // language=PostgreSQL
    @Query("""SELECT f.*, fe.id AS eksternid_id
                    FROM fagsak f
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
                    JOIN behandling b 
                        ON b.fagsak_id = f.id 
                    WHERE b.id = :behandlingId""")
    fun finnFagsakTilBehandling(behandlingId: UUID): FagsakDomain?

    // language=PostgreSQL
    @Query("""SELECT DISTINCT f.*, fe.id AS eksternid_id FROM fagsak f 
                JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
                JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id 
              WHERE ident IN (:personIdenter)""")
    fun findBySøkerIdent(personIdenter: Set<String>): List<FagsakDomain>

    fun findByFagsakPersonId(fagsakPersonId: UUID): List<FagsakDomain>

    // language=PostgreSQL
    @Query("""SELECT f.*, fe.id AS eksternid_id         
                    FROM fagsak f         
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id       
                    WHERE fe.id = :eksternId""")
    fun finnMedEksternId(eksternId: Long): FagsakDomain?

    // language=PostgreSQL
    @Query("""SELECT pi.ident FROM fagsak f
                JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id
              WHERE f.id=:fagsakId
              ORDER BY pi.endret_tid DESC
              LIMIT 1""")
    fun finnAktivIdent(fagsakId: UUID): String

    // language=PostgreSQL
    @Query("""
        SELECT DISTINCT f.id AS first, 
            FIRST_VALUE(ident) OVER (PARTITION BY pi.fagsak_person_id ORDER BY pi.endret_tid DESC) AS second
        FROM fagsak f
          JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id
        WHERE f.id IN (:fagsakIder)""")
    fun finnAktivIdenter(fagsakIder: Set<UUID>): List<Pair<UUID, String>>

}
