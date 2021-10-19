package no.nav.familie.ef.sak.tilkjentytelse

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID


data class TilkjentYtelseDto(val behandlingId: UUID,
                             val vedtakstidspunkt: LocalDateTime,
                             val andeler: List<AndelTilkjentYtelseDto>)

data class AndelTilkjentYtelseDto(val beløp: Int,
                                  val stønadFra: LocalDate,
                                  val stønadTil: LocalDate,
                                  val inntekt: Int,
                                  val inntektsreduksjon: Int,
                                  val samordningsfradrag: Int)