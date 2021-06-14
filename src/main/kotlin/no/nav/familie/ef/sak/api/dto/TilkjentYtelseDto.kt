package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate
import java.util.UUID


data class TilkjentYtelseDto(val behandlingId: UUID, val vedtaksdato: LocalDate?, val andeler: List<AndelTilkjentYtelseDto>)

data class AndelTilkjentYtelseDto(val beløp: Int, val stønadFra: LocalDate, val stønadTil: LocalDate, val inntekt: Int, val samordningsfradrag: Int)