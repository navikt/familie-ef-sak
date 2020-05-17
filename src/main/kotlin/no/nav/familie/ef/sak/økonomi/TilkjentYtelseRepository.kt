package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelseStatus
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.util.*

interface TilkjentYtelseRepository : CrudRepository<TilkjentYtelse,Long> {

    @Query(value = "SELECT * FROM Tilkjent_Ytelse ty WHERE ty.ekstern_id = :eksternId")
    fun findByEksternIdOrNull(eksternId: UUID): TilkjentYtelse?

    @Query(value = "SELECT * FROM Tilkjent_Ytelse ty WHERE ty.personIdent = :personIdent")
    fun findByPersonIdentifikatorOrNull(personIdent: String): TilkjentYtelse?

}
