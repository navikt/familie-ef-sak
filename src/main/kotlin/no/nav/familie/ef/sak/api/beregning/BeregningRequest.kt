package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.util.Periode
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningRequest(val inntektsperioder: List<Inntektsperiode>, val vedtaksperiode: List<Periode>)

data class Inntektsperiode(val startDato: LocalDate, val sluttDato: LocalDate, val inntekt: BigDecimal, val samordningsfradrag: BigDecimal)