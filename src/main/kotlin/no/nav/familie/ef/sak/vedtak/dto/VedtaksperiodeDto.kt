package no.nav.familie.ef.sak.vedtak.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.YearMonth

data class VedtaksperiodeDto(
    @Deprecated("Bruk periode", ReplaceWith("periode.fom")) val årMånedFra: YearMonth? = null,
    @Deprecated("Bruk periode", ReplaceWith("periode.tom")) val årMånedTil: YearMonth? = null,
    @JsonIgnore
    val periode: Månedsperiode =
        Månedsperiode(
            årMånedFra ?: error("periode eller årMånedFra må ha verdi"),
            årMånedTil ?: error("periode eller årMånedTil må ha verdi"),
        ),
    val aktivitet: AktivitetType,
    val periodeType: VedtaksperiodeType,
    val sanksjonsårsak: Sanksjonsårsak? = null,
) {
    fun erMidlertidigOpphørEllerSanksjon(): Boolean = periodeType.midlertidigOpphørEllerSanksjon()
}

fun List<VedtaksperiodeDto>.tilPerioder(): List<Månedsperiode> =
    this.map {
        it.periode
    }

fun List<VedtaksperiodeDto>.tilDomene(): List<Vedtaksperiode> =
    this.map {
        Vedtaksperiode(
            periode = it.periode,
            aktivitet = it.aktivitet,
            periodeType = it.periodeType,
            sanksjonsårsak = it.sanksjonsårsak,
        )
    }

fun List<Vedtaksperiode>.fraDomene(): List<VedtaksperiodeDto> =
    this.map {
        VedtaksperiodeDto(
            årMånedFra = it.periode.fom,
            årMånedTil = it.periode.tom,
            periode = it.periode,
            aktivitet = it.aktivitet,
            periodeType = it.periodeType,
            sanksjonsårsak = it.sanksjonsårsak,
        )
    }
