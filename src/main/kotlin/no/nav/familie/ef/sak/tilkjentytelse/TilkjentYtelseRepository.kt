package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import org.springframework.data.jdbc.repository.query.Query
import java.time.LocalDate
import java.util.UUID

interface TilkjentYtelseRepository : RepositoryInterface<TilkjentYtelse, UUID>, InsertUpdateRepository<TilkjentYtelse> {

    fun findByPersonident(personident: String): TilkjentYtelse?

    fun findByBehandlingId(behandlingId: UUID): TilkjentYtelse?

    // language=PostgreSQL
    @Query("""
        SELECT ty.*
        FROM tilkjent_ytelse ty
            JOIN behandling b ON b.id = ty.behandling_id
        WHERE b.fagsak_id = :fagsakId
         AND b.status = 'FERDIGSTILT'
         AND b.type != 'BLANKETT'
         AND b.resultat IN ('OPPHØRT', 'INNVILGET') 
        ORDER BY b.opprettet_tid ASC""")
    fun finnAlleIverksatteForFagsak(fagsakId: UUID): List<TilkjentYtelse>

    // language=PostgreSQL
    @Query("""
        SELECT ty.*
        FROM tilkjent_ytelse ty 
        WHERE ty.behandling_id IN (SELECT id FROM gjeldende_iverksatte_behandlinger WHERE stonadstype=:stønadstype) 
         AND EXISTS (SELECT 1 FROM andel_tilkjent_ytelse aty 
                        WHERE ty.id = aty.tilkjent_ytelse
                         AND aty.stonad_tom >= :datoForAvstemming
                         AND aty.belop > 0)
          """)
    fun finnTilkjentYtelserTilKonsistensavstemming(stønadstype: Stønadstype, datoForAvstemming: LocalDate): List<TilkjentYtelse>

}
