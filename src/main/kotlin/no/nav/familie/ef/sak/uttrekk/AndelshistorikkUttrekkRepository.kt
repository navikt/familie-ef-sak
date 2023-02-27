package no.nav.familie.ef.sak.uttrekk

import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AndelshistorikkUttrekkRepository : RepositoryInterface<UttrekkFagsakMedAndelshistorikk, UUID> {

    @Query(
        """
         select distinct(f.id)
         from vedtak v
         join behandling b on b.id = v.behandling_id
         join fagsak f on b.fagsak_id = f.id
         where perioder::text like '%FORSØRGER_MANGLER_TILSYNSORDNING%'
        """
    )
    fun finnFagsakerMedTilsynManglerKandidater(): List<UUID>
}
