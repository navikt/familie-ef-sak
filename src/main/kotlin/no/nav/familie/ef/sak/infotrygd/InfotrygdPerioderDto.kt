package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdAktivitetstype
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.felles.Månedsperiode
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
    val stønadsperiode: Månedsperiode,
    @Deprecated("Bruk stønadsperiode", ReplaceWith("stønadsperiode.fom")) val stønadFom: LocalDate = stønadsperiode.fomDato,
    @Deprecated("Bruk stønadsperiode", ReplaceWith("stønadsperiode.tom")) val stønadTom: LocalDate = stønadsperiode.tomDato,
    val opphørsdato: LocalDate?,
    val inntektsgrunnlag: Int,
    val inntektsreduksjon: Int,
    val samordningsfradrag: Int,
    val utgifterBarnetilsyn: Int,
    val månedsbeløp: Int,
    val engangsbeløp: Int,
    val aktivitet: InfotrygdAktivitetstype?
)

fun InfotrygdPeriode.tilSummertInfotrygdperiodeDto(): SummertInfotrygdPeriodeDto =
    SummertInfotrygdPeriodeDto(
        stønadFom = this.stønadFom,
        stønadTom = this.stønadTom,
        stønadsperiode = Månedsperiode(this.stønadFom, this.stønadTom),
        opphørsdato = this.opphørsdato,
        inntektsgrunnlag = this.inntektsgrunnlag,
        inntektsreduksjon = this.inntektsreduksjon,
        samordningsfradrag = this.samordningsfradrag,
        utgifterBarnetilsyn = this.utgifterBarnetilsyn,
        månedsbeløp = this.månedsbeløp,
        engangsbeløp = this.engangsbeløp,
        aktivitet = this.aktivitetstype
    )
