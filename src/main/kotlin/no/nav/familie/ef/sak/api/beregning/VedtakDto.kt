package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.repository.domain.Vedtaksperiode
import java.math.BigDecimal
import java.time.YearMonth

enum class ResultatType {
    INNVILGE,
    AVSLÅ,
    HENLEGGE
}

data class VedtakDto(val resultatType: ResultatType,
                     val periodeBegrunnelse: String,
                     val inntektBegrunnelse: String,
                     val perioder: List<VedtaksperiodeDto> = emptyList(),
                     val inntekter: List<InntektsperiodeDto> = emptyList())

data class VedtaksperiodeDto(
        val årMånedFra: YearMonth,
        val årMånedTil: YearMonth,
        val aktivitet: String,
        val periodeType: String
)

data class InntektsperiodeDto(
        val årMånedFra: YearMonth,
        val forventetInntekt: BigDecimal,
        val samordningsfradrag: BigDecimal?
)


fun Inntektsperiode.fraDomene(): InntektsperiodeDto = InntektsperiodeDto(
        årMånedFra = YearMonth.from(this.startDato),
        forventetInntekt = this.inntekt,
        samordningsfradrag = this.samordningsfradrag
)

fun InntektsperiodeDto.tilDomene(): Inntektsperiode = Inntektsperiode(
        startDato = this.årMånedFra.atDay(1),
        inntekt = this.forventetInntekt,
        samordningsfradrag = this.samordningsfradrag
)

fun Vedtaksperiode.fraDomene(): VedtaksperiodeDto = VedtaksperiodeDto(
        årMånedFra = YearMonth.from(this.datoFra),
        årMånedTil = YearMonth.from(this.datoTil),
        aktivitet = this.aktivitet,
        periodeType = this.periodeType,
)

fun VedtaksperiodeDto.tilDomene(): Vedtaksperiode = Vedtaksperiode(
        datoFra = this.årMånedFra.atDay(1),
        datoTil = this.årMånedTil.atEndOfMonth(),
        aktivitet = this.aktivitet,
        periodeType = this.periodeType,
)
