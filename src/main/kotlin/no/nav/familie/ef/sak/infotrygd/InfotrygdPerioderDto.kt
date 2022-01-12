package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import java.time.LocalDate

data class InfotrygdPerioderDto(
        val overgangsstønad: InfotrygdPerioder,
        val barnetilsyn: InfotrygdPerioder,
        val skolepenger: InfotrygdPerioder
)

data class InfotrygdPerioder(
        val perioder: List<InfotrygdPeriode>,
        val summert: List<SummertInfotrygdPeriode>
)

data class SummertInfotrygdPeriode(
        val stønadFom: LocalDate,
        val stønadTom: LocalDate,
        val opphørsdato: LocalDate?,
        val inntektsreduksjon: Int,
        val samordningsfradrag: Int,
        val beløp: Int
)

fun InternPeriode.tilSummertInfotrygdperiodeDto(): SummertInfotrygdPeriode =
        SummertInfotrygdPeriode(
                stønadFom = this.stønadFom,
                stønadTom = this.stønadTom,
                opphørsdato = this.opphørsdato,
                inntektsreduksjon = this.inntektsreduksjon, // TODO burde denne egentlige endres til inntekt til dto?
                samordningsfradrag = this.samordningsfradrag,
                beløp = this.beløp
        )