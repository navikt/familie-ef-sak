package no.nav.familie.ef.sak.tilkjentytelse.domain

import no.nav.familie.kontrakter.felles.Periode
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.util.UUID

data class AndelTilkjentYtelse(
    @Column("belop")
    val beløp: Int,
    @Column("stonad_fom")
    val stønadFom: LocalDate,
    @Column("stonad_tom")
    val stønadTom: LocalDate,
    val personIdent: String,
    val inntekt: Int,
    val inntektsreduksjon: Int,
    val samordningsfradrag: Int,
    val kildeBehandlingId: UUID
) {

    constructor(
        beløp: Int,
        periode: Periode,
        personIdent: String,
        inntekt: Int,
        inntektsreduksjon: Int,
        samordningsfradrag: Int,
        kildeBehandlingId: UUID
    ) : this(
        beløp,
        periode.fomDato,
        periode.tomDato,
        personIdent,
        inntekt,
        inntektsreduksjon,
        samordningsfradrag,
        kildeBehandlingId
    )

    fun erStønadOverlappende(fom: LocalDate): Boolean = this.periode.inneholder(fom)

    val periode get() = Periode(stønadFom, stønadTom)
}
