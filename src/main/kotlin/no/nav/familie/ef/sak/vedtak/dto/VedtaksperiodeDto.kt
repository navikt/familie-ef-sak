package no.nav.familie.ef.sak.vedtak.dto

import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.felles.util.erPåfølgende
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import java.time.YearMonth

data class VedtaksperiodeDto(
        val årMånedFra: YearMonth,
        val årMånedTil: YearMonth,
        val aktivitet: AktivitetType,
        val periodeType: VedtaksperiodeType
) {

    fun tilPeriode() = Periode(fradato = this.årMånedFra.atDay(1),
                               tildato = this.årMånedTil.atEndOfMonth())
}

fun List<VedtaksperiodeDto>.erSammenhengende(): Boolean = this.foldIndexed(true) { index, acc, periode ->
    if (index == 0) {
        acc
    } else {
        val forrigePeriode = this[index - 1]
        when {
            forrigePeriode.årMånedTil.erPåfølgende(periode.årMånedFra) -> acc
            else -> false
        }
    }
}

fun List<VedtaksperiodeDto>.tilPerioder(): List<Periode> =
        this.map {
            it.tilPeriode()
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

fun List<Vedtaksperiode>.fraDomene(): List<VedtaksperiodeDto> =
        this.map {
            VedtaksperiodeDto(
                    årMånedFra = YearMonth.from(it.datoFra),
                    årMånedTil = YearMonth.from(it.datoTil),
                    aktivitet = it.aktivitet,
                    periodeType = it.periodeType,
            )
        }

fun List<Vedtaksperiode>.fraDomeneForSanksjon(): VedtaksperiodeDto =
        VedtaksperiodeDto(
                årMånedFra = YearMonth.from(this.first().datoFra),
                årMånedTil = YearMonth.from(this.first().datoTil),
                aktivitet = this.first().aktivitet,
                periodeType = this.first().periodeType,
        )
