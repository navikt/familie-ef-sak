package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdAktivitetstype
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
        val utgifterBarnetilsyn: Int,
        @Deprecated("Bruk månedsbeløp / engangsbeløp")
        val beløp: Int,
        val månedsbeløp: Int,
        val engangsbeløp: Int,
        val aktivitet: InfotrygdAktivitetstype?
)

fun InfotrygdPeriode.tilSummertInfotrygdperiodeDto(): SummertInfotrygdPeriodeDto =
        SummertInfotrygdPeriodeDto(
                stønadFom = this.stønadFom,
                stønadTom = this.stønadTom,
                opphørsdato = this.opphørsdato,
                inntektsgrunnlag = this.inntektsgrunnlag,
                inntektsreduksjon = this.inntektsreduksjon,
                samordningsfradrag = this.samordningsfradrag,
                utgifterBarnetilsyn = this.utgifterBarnetilsyn,
                beløp = this.månedsbeløp,
                månedsbeløp = this.månedsbeløp,
                engangsbeløp = this.engangsbeløp,
                aktivitet = this.aktivitetstype
        )