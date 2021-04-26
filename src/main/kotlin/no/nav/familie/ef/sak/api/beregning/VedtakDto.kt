package no.nav.familie.ef.sak.api.beregning

import no.nav.familie.ef.sak.repository.domain.Vedtaksperiode
import no.nav.familie.ef.sak.util.Periode
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
                     val inntekter: List<Inntekt> = emptyList())

data class VedtaksperiodeDto(
        val årMånedFra: YearMonth,
        val årMånedTil: YearMonth,
        val aktivitet: String,
        val periodeType: String
)


fun List<Vedtaksperiode>.fraDomene(): List<VedtaksperiodeDto> =
        this.map {
            VedtaksperiodeDto(
                    årMånedFra = YearMonth.from(it.datoFra),
                    årMånedTil = YearMonth.from(it.datoTil),
                    aktivitet = it.aktivitet,
                    periodeType = it.periodeType,
            )
        }

fun List<VedtaksperiodeDto>.tilDomene(): List<Vedtaksperiode> =
        this.map {
            Vedtaksperiode(
                    datoFra = it.årMånedFra.atDay(1),
                    datoTil = it.årMånedTil.atEndOfMonth(),
                    aktivitet = it.aktivitet,
                    periodeType = it.periodeType,
            )
        }

fun List<VedtaksperiodeDto>.tilPerioder(): List<Periode> =
        this.map {
            Periode(
                    fradato = it.årMånedFra.atDay(1),
                    tildato = it.årMånedTil.atEndOfMonth(),
            )
        }