package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface GjeldendeBarnRepository : RepositoryInterface<BarnTilUtplukkForOppgave, UUID>,
                                    InsertUpdateRepository<BarnTilUtplukkForOppgave> {

    // language=PostgreSQL
    @Query("""
        SELECT b.id behandling_id, s.fodselsnummer fodselsnummer_soker, b2.fodselsnummer fodselsnummer_barn, b2.fodsel_termindato 
        FROM gjeldende_iverksatte_behandlinger b
            JOIN grunnlag_soknad gs ON gs.behandling_id = b.id
            JOIN soknadsskjema s ON gs.soknadsskjema_id = s.id
            JOIN soknad_barn b2 ON s.id = b2.soknadsskjema_id
        WHERE  b.stonadstype=:stønadstype AND EXISTS(SELECT 1 FROM andel_tilkjent_ytelse aty
            JOIN tilkjent_ytelse ty ON aty.tilkjent_ytelse = ty.id
            WHERE ty.id = aty.tilkjent_ytelse 
            AND ty.behandling_id = b.id
          AND aty.stonad_tom >= :dato
          AND aty.belop > 0)
        """)
    fun finnBarnAvGjeldendeIverksatteBehandlinger(stønadstype: Stønadstype, dato: LocalDate): List<BarnTilUtplukkForOppgave>
}
