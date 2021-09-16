package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.repository.domain.AktivitetType
import no.nav.familie.ef.sak.repository.domain.Vedtaksperiode
import no.nav.familie.ef.sak.repository.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.util.Periode
import java.time.YearMonth

data class VedtaksperiodeDto(
        val årMånedFra: YearMonth,
        val årMånedTil: YearMonth,
        val aktivitet: AktivitetType,
        val periodeType: VedtaksperiodeType
)

fun List<VedtaksperiodeDto>.tilPerioder(): List<Periode> =
        this.map {
            Periode(
                    fradato = it.årMånedFra.atDay(1),
                    tildato = it.årMånedTil.atEndOfMonth(),
            )
        }

fun ÅrMånedPeriode.tilPerioder(): Periode =
        Periode(
                fradato = this.årMånedFra.atDay(1),
                tildato = this.årMånedTil.atEndOfMonth(),
        )


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