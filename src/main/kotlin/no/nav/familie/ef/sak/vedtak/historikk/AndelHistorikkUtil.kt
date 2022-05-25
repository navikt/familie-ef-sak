package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.felles.util.harPåfølgendeMåned

object AndelHistorikkUtil {

    fun List<AndelHistorikkDto>.slåSammen(harSammeVerdi: (AndelHistorikkDto, AndelHistorikkDto) -> Boolean): List<AndelHistorikkDto> {
        return this.fold(mutableListOf()) { acc, entry ->
            val last = acc.lastOrNull()
            if (last != null && harSammeVerdi(last, entry)) {
                acc.removeLast()
                acc.add(last.copy(andel = last.andel.copy(stønadTil = entry.andel.stønadTil)))
            } else {
                acc.add(entry)
            }
            acc
        }
    }

    fun sammenhengende(first: AndelHistorikkDto,
                       second: AndelHistorikkDto) =
            first.andel.stønadTil.harPåfølgendeMåned(second.andel.stønadFra)

}