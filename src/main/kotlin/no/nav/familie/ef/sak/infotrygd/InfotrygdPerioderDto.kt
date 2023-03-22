package no.nav.familie.ef.sak.infotrygd

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdAktivitetstype
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.LocalDate

data class InfotrygdPerioderDto(
    val overgangsstønad: InfotrygdStønadPerioderDto,
    val barnetilsyn: InfotrygdStønadPerioderDto,
    val skolepenger: InfotrygdStønadPerioderDto,
)

data class InfotrygdStønadPerioderDto(
    val perioder: List<InfotrygdPeriode>,
    val summert: List<SummertInfotrygdPeriodeDto>,
)

data class SummertInfotrygdPeriodeDto(
    val stønadsperiode: Månedsperiode,
    val opphørsdato: LocalDate?,
    val inntektsgrunnlag: Int,
    val inntektsreduksjon: Int,
    val samordningsfradrag: Int,
    val utgifterBarnetilsyn: Int,
    val månedsbeløp: Int,
    val engangsbeløp: Int,
    val aktivitet: InfotrygdAktivitetstype?,
    val barnIdenter: List<String>,
) {

    @Deprecated("Bruk stønadsperiode", ReplaceWith("stønadsperiode.fom"))
    @get:JsonProperty
    val stønadFom: LocalDate get() = stønadsperiode.fomDato

    @Deprecated("Bruk stønadsperiode", ReplaceWith("stønadsperiode.tom"))
    @get:JsonProperty
    val stønadTom: LocalDate get() = stønadsperiode.tomDato
}

fun InfotrygdPeriode.tilSummertInfotrygdperiodeDto(): SummertInfotrygdPeriodeDto =
    SummertInfotrygdPeriodeDto(
        stønadsperiode = Månedsperiode(this.stønadFom, this.stønadTom),
        opphørsdato = this.opphørsdato,
        inntektsgrunnlag = this.inntektsgrunnlag,
        inntektsreduksjon = this.inntektsreduksjon,
        samordningsfradrag = this.samordningsfradrag,
        utgifterBarnetilsyn = this.utgifterBarnetilsyn,
        månedsbeløp = this.månedsbeløp,
        engangsbeløp = this.engangsbeløp,
        aktivitet = this.aktivitetstype,
        barnIdenter = this.barnIdenter,
    )
