package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate

data class AndelTilkjentYtelse(@Column("belop")
                               val beløp: Int,
                               @Column("stonad_fom")
                               val stønadFom: LocalDate,
                               @Column("stonad_tom")
                               val stønadTom: LocalDate,
                               val type: YtelseType)

enum class YtelseType(val klassifisering: String) {
    OVERGANGSSTØNAD("??")
}