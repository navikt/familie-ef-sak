package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import java.time.LocalDate
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
        return if (vedtak.resultatType == ResultatType.INNVILGE || vedtak.resultatType == ResultatType.INNVILGE_MED_OPPHØR) {
            val nyePerioder = vedtak.perioder?.perioder ?: error("Finner ikke perioder på vedtak=${vedtak.behandlingId}")
            val førsteFraDato = nyePerioder.first().datoFra
            avkortTidligerePerioder(acc.lastOrNull(), førsteFraDato) + nyePerioder
        } else {
            val opphørFom = vedtak.opphørFom ?: error("Mangler dato for opphør på vedtak=${vedtak.behandlingId}")
            avkortTidligerePerioder(acc.lastOrNull(), opphørFom)
        }
    }

    /**
     * Då ett nytt vedtak splitter tidligere vedtaksperioder,
     * så må vi avkorte tidligere periode, då det nye vedtaket overskrever det seneste
     */
    private fun avkortTidligerePerioder(sisteVedtak: Pair<UUID, List<Vedtaksperiode>>?,
                                        datoSomTidligerePeriodeOpphør: LocalDate): List<Vedtaksperiode> {
        if (sisteVedtak == null) return emptyList()
        return sisteVedtak.second.mapNotNull {
            if (it.datoFra >= datoSomTidligerePeriodeOpphør) {
                null
            } else if (it.datoTil < datoSomTidligerePeriodeOpphør) {
                it
            } else {
                it.copy(datoTil = datoSomTidligerePeriodeOpphør.minusDays(1))
            }
        }
    }

    private fun sorterVedtak(vedtaksliste: List<Vedtak>, datoOpprettetPerBehandling: Map<UUID, LocalDateTime>): List<Vedtak> {
        return vedtaksliste.map { it to datoOpprettetPerBehandling.getValue(it.behandlingId) }
                .sortedBy { it.second }
                .map { it.first }
    }
}