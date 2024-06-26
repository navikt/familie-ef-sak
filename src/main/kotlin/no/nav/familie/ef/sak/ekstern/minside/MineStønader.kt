package no.nav.familie.ef.sak.ekstern.minside

import java.time.LocalDate

data class MineStønaderDto(
    val overgangsstønad: List<StønadsperiodeDto>,
    val barnetilsyn: List<StønadsperiodeDto>,
    val skolepenger: List<StønadsperiodeDto>,
)

data class StønadsperiodeDto(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val beløp: Int,
    val inntektsgrunnlag: Int,
    val samordningsfradrag: Int,
)
