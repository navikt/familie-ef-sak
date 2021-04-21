package no.nav.familie.ef.sak.api.beregning

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class BeregningRequest(val inntektsPerioder: List<Inntektsperiode>, val stønadFom: LocalDate, val stønadTom: LocalDate)

data class Inntektsperiode(val startDato: LocalDate, val inntekt: BigDecimal, val samordningsfradrag: BigDecimal? = null)