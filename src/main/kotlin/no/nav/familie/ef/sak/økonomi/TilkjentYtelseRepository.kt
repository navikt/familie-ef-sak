package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface TilkjentYtelseRepository : CrudRepository<TilkjentYtelse,Long> {

    @Query(value = "SELECT * FROM Tilkjent_Ytelse aty WHERE aty.ekstern_id = :eksternId")
    fun findByEksternIdOrNull(eksternId: UUID): TilkjentYtelse?

}
