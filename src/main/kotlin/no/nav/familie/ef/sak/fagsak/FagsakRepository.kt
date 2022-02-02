package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakDao
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FagsakRepository : RepositoryInterface<FagsakDao, UUID>, InsertUpdateRepository<FagsakDao> {

    // language=PostgreSQL
    @Query("""SELECT distinct f.*, fe.id AS eksternid_id
                    FROM fagsak f 
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
                    LEFT JOIN fagsak_person_old fp 
                    ON fp.fagsak_id = f.id 
                    WHERE ident IN (:personIdenter)
                    AND stonadstype = :stønadstype""")
    fun findBySøkerIdent(personIdenter: Set<String>, stønadstype: Stønadstype): FagsakDao?

    // language=PostgreSQL
    @Query("""SELECT f.*, fe.id AS eksternid_id
                    FROM fagsak f
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
                    JOIN behandling b 
                        ON b.fagsak_id = f.id 
                    WHERE b.id = :behandlingId""")
    fun finnFagsakTilBehandling(behandlingId: UUID): FagsakDao?

    // language=PostgreSQL
    @Query("""SELECT distinct f.*, fe.id AS eksternid_id FROM fagsak f 
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
                    LEFT JOIN fagsak_person_old fp 
                        ON fp.fagsak_id = f.id 
                     WHERE ident in (:personIdenter)""")
    fun findBySøkerIdent(personIdenter: Set<String>): List<FagsakDao>

    // language=PostgreSQL
    @Query("""SELECT f.*, fe.id AS eksternid_id         
                    FROM fagsak f         
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id       
                    WHERE fe.id = :eksternId""")
    fun finnMedEksternId(eksternId: Long): FagsakDao?

    // language=PostgreSQL
    @Query("""SELECT fp.ident FROM fagsak_person_old fp
                    WHERE fp.fagsak_id=:id
                    ORDER BY fp.endret_tid DESC
                    LIMIT 1""")
    fun finnAktivIdent(id: UUID): String

    // language=PostgreSQL
    @Query("""
        SELECT DISTINCT fagsak_id AS first, FIRST_VALUE(ident) OVER (PARTITION BY fagsak_id ORDER BY endret_tid DESC) AS second
          FROM fagsak_person_old
        WHERE fagsak_id IN (:ider)""")
    fun finnAktivIdenter(ider: Set<UUID>): List<Pair<UUID, String>>

}
