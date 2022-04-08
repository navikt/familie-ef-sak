package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonert
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.util.toYearMonth
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

object VedtakHistorikkBeregner {

    fun lagVedtaksperioderPerBehandling(vedtaksliste: List<AndelHistorikkBeregner.BehandlingVedtakDto>,
                                        datoOpprettetPerBehandling: Map<UUID, LocalDateTime>): Map<UUID, List<VedtaksperiodeDto>> {
        val sorterteVedtak = sorterVedtak(vedtaksliste, datoOpprettetPerBehandling)
        return sorterteVedtak.fold(listOf<Pair<UUID, List<VedtaksperiodeDto>>>()) { acc, vedtak ->
            acc + Pair(vedtak.behandlingId, lagTotalbildeForNyttVedtak(vedtak.vedtakDto, acc))
        }.toMap()
    }

    private fun lagTotalbildeForNyttVedtak(vedtak: VedtakDto,
                                           acc: List<Pair<UUID, List<VedtaksperiodeDto>>>): List<VedtaksperiodeDto> {

        return if (vedtak is InnvilgelseOvergangsstønad) {
                val nyePerioder = vedtak.perioder
                val førsteFraDato = nyePerioder.first().årMånedFra
                avkortTidligerePerioder(acc.lastOrNull(), førsteFraDato) + nyePerioder
            
        } else if (vedtak is Sanksjonert) {
            splitOppPerioderSomErSanksjonert(acc, vedtak)
        } else if (vedtak is Opphør) {
            val opphørFom = vedtak.opphørFom
            avkortTidligerePerioder(acc.lastOrNull(), opphørFom)
        } else {
            emptyList() //TODO
        }
    }

    private fun splitOppPerioderSomErSanksjonert(acc: List<Pair<UUID, List<VedtaksperiodeDto>>>,
                                                 vedtak: Sanksjonert): List<VedtaksperiodeDto> {
        val sanksjonsperiode = vedtak.periode.tilPeriode()

        return acc.last().second.flatMap {
            val annen = it.tilPeriode()

            if (!sanksjonsperiode.overlapper(annen)) {
                return@flatMap listOf(it, vedtak.periode)
            }

            val nyePerioder = mutableListOf<VedtaksperiodeDto>()
            if (sanksjonsperiode.fradato <= annen.fradato && sanksjonsperiode.tildato < annen.tildato) {
                nyePerioder.add(vedtak.periode)
                nyePerioder.add(it.copy(årMånedFra = sanksjonsperiode.tildato.toYearMonth().plusMonths(1)))
            } else if (sanksjonsperiode.fradato > annen.fradato) {
                nyePerioder.add(it.copy(årMånedTil = sanksjonsperiode.fradato.toYearMonth().minusMonths(1)))
                nyePerioder.add(vedtak.periode)
                if (sanksjonsperiode.tildato < annen.tildato) {
                    nyePerioder.add(it.copy(årMånedTil = sanksjonsperiode.tildato.toYearMonth().plusMonths(1)))
                }
            }
            nyePerioder
        }
    }

    /**
     * Då ett nytt vedtak splitter tidligere vedtaksperioder,
     * så må vi avkorte tidligere periode, då det nye vedtaket overskrever det seneste
     */
    private fun avkortTidligerePerioder(sisteVedtak: Pair<UUID, List<VedtaksperiodeDto>>?,
                                        datoSomTidligerePeriodeOpphør: YearMonth): List<VedtaksperiodeDto> {
        if (sisteVedtak == null) return emptyList()
        return sisteVedtak.second.mapNotNull {
            if (it.årMånedFra >= datoSomTidligerePeriodeOpphør) {
                null
            } else if (it.årMånedTil < datoSomTidligerePeriodeOpphør) {
                it
            } else {
                it.copy(årMånedTil = datoSomTidligerePeriodeOpphør.minusMonths(1))
            }
        }
    }

    private fun sorterVedtak(vedtaksliste: List<AndelHistorikkBeregner.BehandlingVedtakDto>,
                             datoOpprettetPerBehandling: Map<UUID, LocalDateTime>): List<AndelHistorikkBeregner.BehandlingVedtakDto> {
        return vedtaksliste.map { it to datoOpprettetPerBehandling.getValue(it.behandlingId) }
                .sortedBy { it.second }
                .map { it.first }
    }
}