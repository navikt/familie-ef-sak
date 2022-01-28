package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.vedtak.domain.SamordningsfradagType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID


data class TilkjentYtelseDto(val behandlingId: UUID,
                             val vedtakstidspunkt: LocalDateTime,
                             val andeler: List<AndelTilkjentYtelseDto>,
                             val samordningsfradagType: SamordningsfradagType?)

data class AndelTilkjentYtelseDto(val beløp: Int,
                                  val stønadFra: LocalDate,
                                  val stønadTil: LocalDate,
                                  val inntekt: Int,
                                  val inntektsreduksjon: Int,
                                  val samordningsfradrag: Int)