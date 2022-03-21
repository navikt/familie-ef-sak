package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.felles.dto.Periode
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
        return if (vedtak.resultatType == ResultatType.INNVILGE) {
            val nyePerioder = vedtak.perioder?.perioder ?: error("Finner ikke perioder på vedtak=${vedtak.behandlingId}")
            val førsteFraDato = nyePerioder.first().datoFra
            avkortTidligerePerioder(acc.lastOrNull(), førsteFraDato) + nyePerioder
        } else if (vedtak.resultatType == ResultatType.SANKSJONERE) {
            splitOppPerioderSomErSanksjonert(acc, vedtak)
        } else {
            val opphørFom = vedtak.opphørFom ?: error("Mangler dato for opphør på vedtak=${vedtak.behandlingId}")
            avkortTidligerePerioder(acc.lastOrNull(), opphørFom)
        }
    }

    private fun splitOppPerioderSomErSanksjonert(acc: List<Pair<UUID, List<Vedtaksperiode>>>, vedtak: Vedtak): List<Vedtaksperiode> {
        val vedtaksperiodeSanksjon =
                vedtak.perioder?.perioder?.singleOrNull() ?: error("Sanksjon må ha en periode vedtak=${vedtak.behandlingId}")
        val sanksjonsperiode = Periode(vedtaksperiodeSanksjon.datoFra, vedtaksperiodeSanksjon.datoTil)
        return acc.last().second.flatMap {
            if (!sanksjonsperiode.overlapper(Periode(it.datoFra, it.datoTil))) {
                return@flatMap listOf(it)
            }
            val nyePerioder = mutableListOf<Vedtaksperiode>()
            if (sanksjonsperiode.fradato <= it.datoFra && sanksjonsperiode.tildato < it.datoTil) {
                nyePerioder.add(vedtaksperiodeSanksjon)
                nyePerioder.add(it.copy(datoFra = sanksjonsperiode.tildato.plusDays(1)))
            } else if (sanksjonsperiode.fradato > it.datoFra) {
                nyePerioder.add(it.copy(datoTil = sanksjonsperiode.fradato.minusDays(1)))
                nyePerioder.add(vedtaksperiodeSanksjon)
                if (sanksjonsperiode.tildato < it.datoTil) {
                    nyePerioder.add(it.copy(datoFra = sanksjonsperiode.tildato.plusDays(1)))
                }
            }
            nyePerioder
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