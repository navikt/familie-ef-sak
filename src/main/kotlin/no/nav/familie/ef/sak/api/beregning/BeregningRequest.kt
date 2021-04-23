package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.util.Periode
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningRequest(val inntektsperioder: List<Inntekt>, val vedtaksperiode: List<Periode>)

data class Inntekt(val startDato: LocalDate, val inntekt: BigDecimal, val samordningsfradrag: BigDecimal)
data class Inntektsperiode(val startDato: LocalDate, val sluttDato: LocalDate, val inntekt: BigDecimal, val samordningsfradrag: BigDecimal)
