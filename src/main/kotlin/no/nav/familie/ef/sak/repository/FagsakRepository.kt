package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FagsakRepository : RepositoryInterface<Fagsak, UUID>, InsertUpdateRepository<Fagsak> {

    // language=PostgreSQL
    @Query("""SELECT f.*, fe.id AS eksternid_id
                    FROM fagsak f 
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
                    LEFT JOIN fagsak_person fp 
                    ON fp.fagsak_id = f.id 
                    WHERE ident = :ident 
                    AND stonadstype = :stønadstype""")
    fun findBySøkerIdent(ident: String, stønadstype: Stønadstype): Fagsak?

    // language=PostgreSQL
    @Query("""SELECT f.*, fe.id AS eksternid_id
                    FROM fagsak f
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
                    JOIN behandling b 
                        ON b.fagsak_id = f.id 
                    WHERE b.id = :behandlingId""")
    fun finnFagsakTilBehandling(behandlingId: UUID): Fagsak?

    // language=PostgreSQL
    @Query("""SELECT f.*, fe.id AS eksternid_id FROM fagsak f 
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id
                    LEFT JOIN fagsak_person fp 
                        ON fp.fagsak_id = f.id 
                     WHERE ident = :ident""")
    fun findBySøkerIdent(ident: String): List<Fagsak>

    // language=PostgreSQL
    @Query("""SELECT f.*, fe.id AS eksternid_id         
                    FROM fagsak f         
                    JOIN fagsak_ekstern fe ON fe.fagsak_id = f.id       
                    WHERE fe.id = :eksternId""")
    fun finnMedEksternId(eksternId: Long): Fagsak?

    // language=PostgreSQL
    @Query("""SELECT fp.ident FROM fagsak_person fp
                    WHERE fp.fagsak_id=:id
                    ORDER BY fp.opprettet_tid DESC
                    LIMIT 1""")
    fun finnAktivIdent(id: UUID): String

}
