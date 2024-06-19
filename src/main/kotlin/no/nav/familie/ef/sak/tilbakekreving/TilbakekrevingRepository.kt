package no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface TilbakekrevingRepository :
    RepositoryInterface<Tilbakekreving, UUID>,
    InsertUpdateRepository<Tilbakekreving> {
    // language=PostgreSQL
    @Query(
        """SELECT COUNT(*) from behandling b
                JOIN fagsak f on b.fagsak_id = f.id
                JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id
                JOIN tilbakekreving t ON t.behandling_id = b.id
            WHERE pi.ident = :personIdent 
                AND t.valg = 'OPPRETT_AUTOMATISK'
                AND b.status = 'FERDIGSTILT' 
                AND b.resultat NOT IN ('HENLAGT', 'AVSLÅTT')
                AND b.vedtakstidspunkt > :etterTidspunkt
                ;
         """,
    )
    fun finnAntallTilbakekrevingerValgtEtterGittDatoForPersonIdent(
        personIdent: String,
        etterTidspunkt: LocalDate,
    ): Int
}
