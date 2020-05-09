package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import org.springframework.data.jpa.repository.JpaRepository

interface AndelTilkjentYtelseRepository : JpaRepository<AndelTilkjentYtelse, Long> {
    fun findByTilkjentYtelseId(tilkjentYtelseId: Long): List<AndelTilkjentYtelse>
}