package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.repository.domain.Vedtaksperiode
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
                     val inntekter: List<Inntektsperiode> = emptyList())

data class VedtaksperiodeDto(
        val månedFra: Int,
        val årFra: Int,
        val månedTil: Int,
        val årTil: Int,
        val aktivitet: String,
        val periodeType: String
) {

    companion object {

        fun fraDomene(perioder: List<Vedtaksperiode>): List<VedtaksperiodeDto> {
            return perioder.map {
                VedtaksperiodeDto(
                        månedFra = it.datoFra.monthValue,
                        årFra = it.datoFra.year,
                        månedTil = it.datoTil.monthValue,
                        årTil = it.datoTil.year,
                        aktivitet = it.aktivitet,
                        periodeType = it.periodeType,
                )
            }
        }

        fun tilDomene(perioder: List<VedtaksperiodeDto>): List<Vedtaksperiode> {
            return perioder.map {
                Vedtaksperiode(
                        datoFra = YearMonth.of(it.årFra, it.månedFra).atDay(1),
                        datoTil = YearMonth.of(it.årTil, it.månedTil).atEndOfMonth(),
                        aktivitet = it.aktivitet,
                        periodeType = it.periodeType,
                )
            }
        }

    }
}