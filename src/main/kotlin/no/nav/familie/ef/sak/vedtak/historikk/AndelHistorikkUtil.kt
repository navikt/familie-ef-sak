package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.kontrakter.felles.ef.StønadType

object AndelHistorikkUtil {

    fun List<AndelHistorikkDto>.slåSammen(harSammeVerdi: (AndelHistorikkDto, AndelHistorikkDto) -> Boolean): List<AndelHistorikkDto> {
        return this.fold(mutableListOf()) { acc, entry ->
            val last = acc.lastOrNull()
            if (last != null && harSammeVerdi(last, entry)) {
                acc.removeLast()
                acc.add(last.copy(andel = last.andel.copy(periode = last.andel.periode.copy(tom = entry.andel.periode.tom))))
            } else {
                acc.add(entry)
            }
            acc
        }
    }

    fun sammenhengende(
        first: AndelHistorikkDto,
        second: AndelHistorikkDto
    ) =
        first.andel.periode.påfølgesAv(second.andel.periode)

    fun periodeTypeOvergangsstønad(
        stønadstype: StønadType,
        vedtaksperiode: Vedtakshistorikkperiode
    ): VedtaksperiodeType? = when {
        stønadstype != StønadType.OVERGANGSSTØNAD -> null
        vedtaksperiode is VedtakshistorikkperiodeOvergangsstønad -> vedtaksperiode.periodeType
        vedtaksperiode is Sanksjonsperiode -> VedtaksperiodeType.SANKSJON
        else -> null
    }

    fun periodeTypeBarnetilsyn(
        stønadstype: StønadType,
        vedtaksperiode: Vedtakshistorikkperiode
    ): PeriodetypeBarnetilsyn? = when {
        stønadstype != StønadType.BARNETILSYN -> null
        vedtaksperiode is VedtakshistorikkperiodeBarnetilsyn -> vedtaksperiode.periodetype
        vedtaksperiode is Sanksjonsperiode -> PeriodetypeBarnetilsyn.SANKSJON_1_MND
        else -> null
    }

    fun aktivitetOvergangsstønad(
        stønadstype: StønadType,
        vedtaksperiode: Vedtakshistorikkperiode
    ): AktivitetType? = when {
        stønadstype != StønadType.OVERGANGSSTØNAD -> null
        vedtaksperiode is VedtakshistorikkperiodeOvergangsstønad -> vedtaksperiode.aktivitet
        vedtaksperiode is Sanksjonsperiode -> AktivitetType.IKKE_AKTIVITETSPLIKT
        else -> null
    }
}
