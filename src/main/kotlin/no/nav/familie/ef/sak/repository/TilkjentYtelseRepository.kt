package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseMedMetaData
import org.springframework.data.jdbc.repository.query.Query
import java.util.*

interface TilkjentYtelseRepository : RepositoryInterface<TilkjentYtelse, UUID>, InsertUpdateRepository<TilkjentYtelse> {

    fun findByPersonident(personident: String): TilkjentYtelse?

    @Query(value = "SELECT t.*, be.id as eksternBehandlingId " +
            "FROM tilkjent_ytelse t " +
            "JOIN behandling_ekstern be on t.behandling_id = be.behandling_id " +
            "WHERE t.id = :tilkjentYtelseId"
            )
    fun finn(tilkjentYtelseId: UUID): TilkjentYtelseMedMetaData?

}
