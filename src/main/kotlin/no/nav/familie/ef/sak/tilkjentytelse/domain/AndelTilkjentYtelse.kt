package no.nav.familie.ef.sak.tilkjentytelse.domain

import no.nav.familie.ef.sak.felles.dto.Periode
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.util.*

data class AndelTilkjentYtelse(@Column("belop")
                               val beløp: Int,
                               @Column("stonad_fom")
                               val stønadFom: LocalDate, /// TODO  Gjør nullable
                               @Column("stonad_tom")
                               val stønadTom: LocalDate, /// TODO  Gjør nullable
                               val personIdent: String,
                               val inntekt: Int,
                               val inntektsreduksjon: Int,
                               val samordningsfradrag: Int,
                               val kildeBehandlingId: UUID) {

    fun erStønadOverlappende(fom: LocalDate): Boolean = this.stønadFom < fom && this.stønadTom >= fom

    val periode get() = Periode(stønadFom, stønadTom)
}
