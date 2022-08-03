package no.nav.familie.ef.sak.vedtak.dto

import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.kontrakter.felles.Periode
import java.time.YearMonth

data class VedtaksperiodeDto(
    @Deprecated("Bruk periode", ReplaceWith("periode.fomMåned")) val årMånedFra: YearMonth? = null,
    @Deprecated("Bruk periode", ReplaceWith("periode.tomMåned")) val årMånedTil: YearMonth? = null,
    val periode: Periode = Periode(
        årMånedFra ?: error("periode eller årMånedFra må ha verdi"),
        årMånedTil ?: error("periode eller årMånedTil må ha verdi")
    ),
    val aktivitet: AktivitetType,
    val periodeType: VedtaksperiodeType
)

fun List<VedtaksperiodeDto>.tilPerioder(): List<Periode> =
    this.map {
        it.periode
    }

fun List<VedtaksperiodeDto>.tilDomene(): List<Vedtaksperiode> =
    this.map {
        Vedtaksperiode(
            periode = it.periode,
            aktivitet = it.aktivitet,
            periodeType = it.periodeType,
        )
    }

fun List<Vedtaksperiode>.fraDomene(): List<VedtaksperiodeDto> =
    this.map {
        VedtaksperiodeDto(
            årMånedFra = it.periode.fomMåned,
            årMånedTil = it.periode.tomMåned,
            periode = it.periode,
            aktivitet = it.aktivitet,
            periodeType = it.periodeType,
        )
    }

fun Vedtaksperiode.fraDomeneForSanksjon(): SanksjonertPeriodeDto =
    SanksjonertPeriodeDto(
        årMånedFra = YearMonth.from(datoFra),
        årMånedTil = YearMonth.from(datoTil),
        fomMåned = YearMonth.from(datoFra),
        tomMåned = YearMonth.from(datoTil)

    )
