package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import java.time.LocalDateTime
import java.util.UUID

object VedtakHistorikkBeregner {

    fun lagVedtaksperioderPerBehandling(vedtaksliste: List<Vedtak>,
                                        datoOpprettetPerBehandling: Map<UUID, LocalDateTime>): Map<UUID, List<Vedtaksperiode>> {
        val sorterteVedtak = sorterVedtak(vedtaksliste, datoOpprettetPerBehandling)
        return sorterteVedtak.fold(listOf<Pair<UUID, List<Vedtaksperiode>>>()) { acc, vedtak ->
            acc + Pair(vedtak.behandlingId, lagTotalbildeForNyttVedtak(vedtak, acc))
        }.toMap()
    }

    private fun lagTotalbildeForNyttVedtak(vedtak: Vedtak,
                                           acc: List<Pair<UUID, List<Vedtaksperiode>>>): List<Vedtaksperiode> {
        val nyePerioder = vedtak.perioder!!.perioder
        return avkortTidligerePerioder(acc.lastOrNull(), nyePerioder.first()) + nyePerioder
    }

    /**
     * Då ett nytt vedtak splitter tidligere vedtaksperioder,
     * så må vi avkorte tidligere periode, då det nye vedtaket overskrever det seneste
     */
    private fun avkortTidligerePerioder(sisteVedtak: Pair<UUID, List<Vedtaksperiode>>?,
                                        førsteNyePeriode: Vedtaksperiode): List<Vedtaksperiode> {
        if (sisteVedtak == null) return emptyList()
        return sisteVedtak.second.mapNotNull {
            if (it.datoFra >= førsteNyePeriode.datoFra) {
                null
            } else if (it.datoTil < førsteNyePeriode.datoFra) {
                it
            } else {
                it.copy(datoTil = førsteNyePeriode.datoFra.minusDays(1))
            }
        }
    }

    private fun sorterVedtak(vedtaksliste: List<Vedtak>, datoOpprettetPerBehandling: Map<UUID, LocalDateTime>): List<Vedtak> {
        return vedtaksliste.map { it to datoOpprettetPerBehandling.getValue(it.behandlingId) }
                .sortedBy { it.second }
                .map { it.first }
    }
}