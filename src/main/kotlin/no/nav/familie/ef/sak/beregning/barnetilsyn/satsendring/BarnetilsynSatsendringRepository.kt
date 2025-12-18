package no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring

import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BarnetilsynSatsendringRepository : RepositoryInterface<BarnetilsynSatsendringKandidat, UUID> {
    // language=PostgreSQL
    @Query(
        """
        SELECT DISTINCT gib.fagsak_id
        FROM gjeldende_iverksatte_behandlinger gib
         JOIN tilkjent_ytelse ty ON ty.behandling_id = gib.id
         JOIN andel_tilkjent_ytelse aty ON ty.id = aty.tilkjent_ytelse
        WHERE aty.stonad_tom >= '2026-01-01' AND gib.stonadstype = 'BARNETILSYN'
        """,
    )
    fun finnSatsendringskandidaterForBarnetilsyn(): List<UUID>
}
