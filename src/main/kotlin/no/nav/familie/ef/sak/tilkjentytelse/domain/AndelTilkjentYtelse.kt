package no.nav.familie.ef.sak.tilkjentytelse.domain

import no.nav.familie.kontrakter.felles.Månedsperiode
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class AndelTilkjentYtelse(
    @Column("belop")
    val beløp: Int,
    @Embedded(prefix = "stonad_", onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val periode: Månedsperiode,
    val personIdent: String,
    val inntekt: Int,
    val inntektsreduksjon: Int,
    val samordningsfradrag: Int,
    val kildeBehandlingId: UUID
) {

    fun erStønadOverlappende(fom: LocalDate): Boolean = this.periode.inneholder(YearMonth.from(fom))
}
