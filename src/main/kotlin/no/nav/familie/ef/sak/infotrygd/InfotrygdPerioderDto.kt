package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import java.time.LocalDate

data class InfotrygdPerioderDto(
        val overgangsstønad: InfotrygdStønadPerioderDto,
        val barnetilsyn: InfotrygdStønadPerioderDto,
        val skolepenger: InfotrygdStønadPerioderDto
)

data class InfotrygdStønadPerioderDto(
        val perioder: List<InfotrygdPeriode>,
        val summert: List<SummertInfotrygdPeriodeDto>
)

data class SummertInfotrygdPeriodeDto(
        val stønadFom: LocalDate,
        val stønadTom: LocalDate,
        val opphørsdato: LocalDate?,
        val inntektsgrunnlag: Int,
        val inntektsreduksjon: Int,
        val samordningsfradrag: Int,
        val beløp: Int
)

fun InfotrygdPeriode.tilSummertInfotrygdperiodeDto(): SummertInfotrygdPeriodeDto =
        SummertInfotrygdPeriodeDto(
                stønadFom = this.stønadFom,
                stønadTom = this.stønadTom,
                opphørsdato = this.opphørsdato,
                inntektsgrunnlag = this.inntektsgrunnlag,
                inntektsreduksjon = this.inntektsreduksjon,
                samordningsfradrag = this.samordningsfradrag,
                beløp = this.beløp
        )