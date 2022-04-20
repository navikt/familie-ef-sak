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
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

sealed class Vedtakshistorikkperiode {

    abstract val datoFra: LocalDate
    abstract val datoTil: LocalDate

    abstract fun medFra(datoFra: LocalDate): Vedtakshistorikkperiode
    abstract fun medTil(datoTil: LocalDate): Vedtakshistorikkperiode
}

data class VedtakshistorikkperiodeOvergangsstønad(
        override val datoFra: LocalDate,
        override val datoTil: LocalDate,
        val aktivitet: AktivitetType,
        val periodeType: VedtaksperiodeType
) : Vedtakshistorikkperiode() {

    constructor(periode: VedtaksperiodeDto) :
            this(periode.årMånedFra.atDay(1),
                 periode.årMånedTil.atEndOfMonth(),
                 periode.aktivitet,
                 periode.periodeType)

    override fun medFra(datoFra: LocalDate): Vedtakshistorikkperiode {
        return this.copy(datoFra = datoFra)
    }

    override fun medTil(datoTil: LocalDate): Vedtakshistorikkperiode {
        return this.copy(datoTil = datoTil)
    }
}

data class VedtakshistorikkperiodeBarnetilsyn(
        override val datoFra: LocalDate,
        override val datoTil: LocalDate,
        val kontantstøtte: Int,
        val tilleggsstønad: Int,
        val utgifter: BigDecimal,
        val antallBarn: Int,
        val aktivitetArbeid: SvarId?
) : Vedtakshistorikkperiode() {

    constructor(periode: BeløpsperiodeBarnetilsynDto, aktivitetArbeid: SvarId?) :
            this(periode.periode.fradato,
                 periode.periode.tildato,
                 periode.beregningsgrunnlag.kontantstøttebeløp.toInt(),
                 periode.beregningsgrunnlag.tilleggsstønadsbeløp.toInt(),
                 periode.beregningsgrunnlag.utgifter,
                 periode.beregningsgrunnlag.antallBarn,
                 aktivitetArbeid
            )

    override fun medFra(datoFra: LocalDate): Vedtakshistorikkperiode {
        return this.copy(datoFra = datoFra)
    }

    override fun medTil(datoTil: LocalDate): Vedtakshistorikkperiode {
        return this.copy(datoTil = datoTil)
    }
}

object VedtakHistorikkBeregner {

    private val beregningBarnetilsyn: BeregningBarnetilsynService = BeregningBarnetilsynService()

    private val logger = LoggerFactory.getLogger(javaClass)

    fun lagVedtaksperioderPerBehandling(vedtaksliste: List<AndelHistorikkBeregner.BehandlingHistorikkData>,
                                        datoOpprettetPerBehandling: Map<UUID, LocalDateTime>): Map<UUID, List<Vedtakshistorikkperiode>> {
        val sorterteVedtak = sorterVedtak(vedtaksliste, datoOpprettetPerBehandling)
        return sorterteVedtak.fold(listOf<Pair<UUID, List<Vedtakshistorikkperiode>>>()) { acc, vedtak ->
            acc + Pair(vedtak.behandlingId, lagTotalbildeForNyttVedtak(vedtak, acc))
        }.toMap()
    }

    private fun lagTotalbildeForNyttVedtak(behandlingHistorikkData: AndelHistorikkBeregner.BehandlingHistorikkData,
                                           acc: List<Pair<UUID, List<Vedtakshistorikkperiode>>>): List<Vedtakshistorikkperiode> {

        val vedtak = behandlingHistorikkData.vedtakDto
        return when (vedtak) {
            is InnvilgelseOvergangsstønad -> {
                val nyePerioder = vedtak.perioder.map { VedtakshistorikkperiodeOvergangsstønad(it) }
                val førsteFraDato = nyePerioder.first().datoFra
                avkortTidligerePerioder(acc.lastOrNull(), førsteFraDato) + nyePerioder
            }
            is InnvilgelseBarnetilsyn -> {
                val perioder = beregningBarnetilsyn.beregnYtelseBarnetilsyn(vedtak).map { VedtakshistorikkperiodeBarnetilsyn(it, behandlingHistorikkData.aktivitetArbeid) }
                val førsteFraDato = perioder.first().datoFra
                avkortTidligerePerioder(acc.lastOrNull(), førsteFraDato) + perioder
            }
            is Sanksjonert -> {
                splitOppPerioderSomErSanksjonert(acc, vedtak)
            }
            is Opphør -> {
                val opphørFom = vedtak.opphørFom
                avkortTidligerePerioder(acc.lastOrNull(), opphørFom.atDay(1))
            }
            else -> {
                logger.error("Håndterer ikke ${vedtak::class.java.simpleName}")
                emptyList()
            }
        }
    }

    private fun splitOppPerioderSomErSanksjonert(acc: List<Pair<UUID, List<Vedtakshistorikkperiode>>>,
                                                 vedtak: Sanksjonert): List<Vedtakshistorikkperiode> {
        val vedtaksperiodeSanksjon = VedtakshistorikkperiodeOvergangsstønad(vedtak.periode.årMånedFra.atDay(1),
                                                                            vedtak.periode.årMånedTil.atEndOfMonth(),
                                                                            vedtak.periode.aktivitet,
                                                                            vedtak.periode.periodeType)
        val sanksjonsperiode = vedtak.periode.tilPeriode()
        return acc.last().second.flatMap {
            if (!sanksjonsperiode.overlapper(Periode(it.datoFra, it.datoTil))) {
                return@flatMap listOf(it, vedtaksperiodeSanksjon)
            }
            val nyePerioder = mutableListOf<Vedtakshistorikkperiode>()
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
    private fun avkortTidligerePerioder(sisteVedtak: Pair<UUID, List<Vedtakshistorikkperiode>>?,
                                        datoSomTidligerePeriodeOpphør: LocalDate): List<Vedtakshistorikkperiode> {
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

    private fun sorterVedtak(vedtaksliste: List<AndelHistorikkBeregner.BehandlingHistorikkData>,
                             datoOpprettetPerBehandling: Map<UUID, LocalDateTime>): List<AndelHistorikkBeregner.BehandlingHistorikkData> {
        return vedtaksliste.map { it to datoOpprettetPerBehandling.getValue(it.behandlingId) }
                .sortedBy { it.second }
                .map { it.first }
    }
}