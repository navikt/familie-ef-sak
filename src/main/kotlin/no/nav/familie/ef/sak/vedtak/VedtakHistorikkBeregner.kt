package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynService
import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonert
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object VedtakHistorikkBeregner {

    private val beregningBarnetilsyn: BeregningBarnetilsynService = BeregningBarnetilsynService()

    sealed class Vedtaksinformasjon {

        abstract val datoFra: LocalDate
        abstract val datoTil: LocalDate

        abstract fun medFra(datoFra: LocalDate): Vedtaksinformasjon
        abstract fun medTil(datoTil: LocalDate): Vedtaksinformasjon
    }

    data class VedtaksinformasjonOvergangsstønad(
            override val datoFra: LocalDate,
            override val datoTil: LocalDate,
            val aktivitet: AktivitetType,
            val periodeType: VedtaksperiodeType
    ) : Vedtaksinformasjon() {

        constructor(periode: VedtaksperiodeDto) :
                this(periode.årMånedFra.atDay(1),
                     periode.årMånedTil.atEndOfMonth(),
                     periode.aktivitet,
                     periode.periodeType)

        override fun medFra(datoFra: LocalDate): Vedtaksinformasjon {
            return this.copy(datoFra = datoFra)
        }

        override fun medTil(datoTil: LocalDate): Vedtaksinformasjon {
            return this.copy(datoTil = datoTil)
        }
    }

    data class VedtaksinformasjonBarnetilsyn(
            override val datoFra: LocalDate,
            override val datoTil: LocalDate,
            val kontantstøtte: Int,
            val tilleggsstønad: Int,
            val utgifter: BigDecimal,
            val antallBarn: Int
    ) : Vedtaksinformasjon() {

        constructor(beløpsperiodeBarnetilsynDto: BeløpsperiodeBarnetilsynDto) :
                this(beløpsperiodeBarnetilsynDto.periode.fradato,
                     beløpsperiodeBarnetilsynDto.periode.tildato,
                     beløpsperiodeBarnetilsynDto.beregningsgrunnlag.kontantstøttebeløp.toInt(),
                     beløpsperiodeBarnetilsynDto.beregningsgrunnlag.tilleggsstønadsbeløp.toInt(),
                     beløpsperiodeBarnetilsynDto.beregningsgrunnlag.utgifter,
                     beløpsperiodeBarnetilsynDto.beregningsgrunnlag.antallBarn
                )

        override fun medFra(datoFra: LocalDate): Vedtaksinformasjon {
            return this.copy(datoFra = datoFra)
        }

        override fun medTil(datoTil: LocalDate): Vedtaksinformasjon {
            return this.copy(datoTil = datoTil)
        }
    }

    fun lagVedtaksperioderPerBehandling(vedtaksliste: List<AndelHistorikkBeregner.BehandlingVedtakDto>,
                                        datoOpprettetPerBehandling: Map<UUID, LocalDateTime>): Map<UUID, List<Vedtaksinformasjon>> {
        val sorterteVedtak = sorterVedtak(vedtaksliste, datoOpprettetPerBehandling)
        return sorterteVedtak.fold(listOf<Pair<UUID, List<Vedtaksinformasjon>>>()) { acc, vedtak ->
            acc + Pair(vedtak.behandlingId, lagTotalbildeForNyttVedtak(vedtak.vedtakDto, acc))
        }.toMap()
    }

    private fun lagTotalbildeForNyttVedtak(vedtak: VedtakDto,
                                           acc: List<Pair<UUID, List<Vedtaksinformasjon>>>): List<Vedtaksinformasjon> {

        return if (vedtak is InnvilgelseOvergangsstønad) {
            val nyePerioder = vedtak.perioder.map { VedtaksinformasjonOvergangsstønad(it) }
            val førsteFraDato = nyePerioder.first().datoFra
            avkortTidligerePerioder(acc.lastOrNull(), førsteFraDato) + nyePerioder
        } else if (vedtak is InnvilgelseBarnetilsyn) {
            val perioder = beregningBarnetilsyn.beregnYtelseBarnetilsyn(vedtak).map { VedtaksinformasjonBarnetilsyn(it) }
            val førsteFraDato = perioder.first().datoFra
            avkortTidligerePerioder(acc.lastOrNull(), førsteFraDato) + perioder
        } else if (vedtak is Sanksjonert) {
            splitOppPerioderSomErSanksjonert(acc, vedtak)
        } else if (vedtak is Opphør) {
            val opphørFom = vedtak.opphørFom
            avkortTidligerePerioder(acc.lastOrNull(), opphørFom.atDay(1))
        } else {
            emptyList() //TODO
        }
    }

    private fun splitOppPerioderSomErSanksjonert(acc: List<Pair<UUID, List<Vedtaksinformasjon>>>,
                                                 vedtak: Sanksjonert): List<Vedtaksinformasjon> {
        val vedtaksperiodeSanksjon = VedtaksinformasjonOvergangsstønad(vedtak.periode.årMånedFra.atDay(1),
                                                                       vedtak.periode.årMånedTil.atEndOfMonth(),
                                                                       vedtak.periode.aktivitet,
                                                                       vedtak.periode.periodeType)
        val sanksjonsperiode = vedtak.periode.tilPeriode()
        return acc.last().second.flatMap {
            if (!sanksjonsperiode.overlapper(Periode(it.datoFra, it.datoTil))) {
                return@flatMap listOf(it, vedtaksperiodeSanksjon)
            }
            val nyePerioder = mutableListOf<Vedtaksinformasjon>()
            if (sanksjonsperiode.fradato <= it.datoFra && sanksjonsperiode.tildato < it.datoTil) {
                nyePerioder.add(vedtaksperiodeSanksjon)
                nyePerioder.add(it.medFra(datoFra = sanksjonsperiode.tildato.plusDays(1)))
            } else if (sanksjonsperiode.fradato > it.datoFra) {
                nyePerioder.add(it.medTil(datoTil = sanksjonsperiode.fradato.minusDays(1)))
                nyePerioder.add(vedtaksperiodeSanksjon)
                if (sanksjonsperiode.tildato < it.datoTil) {
                    nyePerioder.add(it.medFra(datoFra = sanksjonsperiode.tildato.plusDays(1)))
                }
            }
            nyePerioder
        }
    }

    /**
     * Då ett nytt vedtak splitter tidligere vedtaksperioder,
     * så må vi avkorte tidligere periode, då det nye vedtaket overskrever det seneste
     */
    private fun avkortTidligerePerioder(sisteVedtak: Pair<UUID, List<Vedtaksinformasjon>>?,
                                        datoSomTidligerePeriodeOpphør: LocalDate): List<Vedtaksinformasjon> {
        if (sisteVedtak == null) return emptyList()
        return sisteVedtak.second.mapNotNull {
            if (it.datoFra >= datoSomTidligerePeriodeOpphør) {
                null
            } else if (it.datoTil < datoSomTidligerePeriodeOpphør) {
                it
            } else {
                it.medTil(datoTil = datoSomTidligerePeriodeOpphør.minusDays(1))
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