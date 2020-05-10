package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.dto.AndelTilkjentYtelse
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface AndelTilkjentYtelseRepository : CrudRepository<AndelTilkjentYtelse, Long> {

    @Query(value = "SELECT * FROM Andel_Tilkjent_Ytelse aty WHERE aty.tilkjentytelse_id_fkey = :tilkjentYtelseId")
    fun findByTilkjentYtelseId(tilkjentYtelseId: Long): List<AndelTilkjentYtelse>
}