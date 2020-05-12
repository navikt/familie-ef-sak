package no.nav.familie.ef.sak.økonomi.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate

data class AndelTilkjentYtelse(
        @Id
        val id: Long = 0,
        @Column("tilkjentytelse_id_fkey")
        val tilkjentYtelseId: Long,
        @Column("personIdent")
        val personIdentifikator : String,
        @Column("belop")
        val beløp: Int,
        @Column("stonad_fom")
        val stønadFom: LocalDate,
        @Column("stonad_tom")
        val stønadTom: LocalDate,
        val type: YtelseType
)

enum class YtelseType(val klassifisering: String) {
        OVERGANGSSTØNAD("??")
}