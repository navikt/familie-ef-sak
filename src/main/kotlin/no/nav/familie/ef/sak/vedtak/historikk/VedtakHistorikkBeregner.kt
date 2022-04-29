package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.tilkjentytelse.tilBeløpsperiodeBarnetilsyn
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
import java.util.UUID

sealed class Vedtakshistorikkperiode {

    abstract val datoFra: LocalDate
    abstract val datoTil: LocalDate
    abstract val erSanksjon: Boolean

    abstract fun medFra(datoFra: LocalDate): Vedtakshistorikkperiode
    abstract fun medTil(datoTil: LocalDate): Vedtakshistorikkperiode

}

data class VedtakshistorikkperiodeOvergangsstønad(
        override val datoFra: LocalDate,
        override val datoTil: LocalDate,
        val aktivitet: AktivitetType,
        val periodeType: VedtaksperiodeType
) : Vedtakshistorikkperiode() {

    override val erSanksjon = periodeType == VedtaksperiodeType.SANKSJON

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
        override val erSanksjon: Boolean,
        val kontantstøtte: Int,
        val tilleggsstønad: Int,
        val utgifter: BigDecimal,
        val antallBarn: Int,
        val aktivitetArbeid: SvarId?,
        val barn: List<UUID>,
        val sats: Int,
        val beløpFørSatsjustering: Int,
) : Vedtakshistorikkperiode() {

    constructor(periode: BeløpsperiodeBarnetilsynDto, aktivitetArbeid: SvarId?) :
            this(
                    periode.periode.fradato,
                    periode.periode.tildato,
                    false,
                    periode.beregningsgrunnlag.kontantstøttebeløp.toInt(),
                    periode.beregningsgrunnlag.tilleggsstønadsbeløp.toInt(),
                    periode.beregningsgrunnlag.utgifter,
                    periode.beregningsgrunnlag.antallBarn,
                    aktivitetArbeid,
                    periode.beregningsgrunnlag.barn,
                    periode.sats,
                    periode.beløpFørFratrekkOgSatsjustering,
            )

    override fun medFra(datoFra: LocalDate): Vedtakshistorikkperiode {
        return this.copy(datoFra = datoFra)
    }

    override fun medTil(datoTil: LocalDate): Vedtakshistorikkperiode {
        return this.copy(datoTil = datoTil)
    }
}

object VedtakHistorikkBeregner {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun lagVedtaksperioderPerBehandling(vedtaksliste: List<BehandlingHistorikkData>)
            : Map<UUID, List<Vedtakshistorikkperiode>> {
        return vedtaksliste
                .sortedBy { it.tilkjentYtelse.sporbar.opprettetTid }
                .fold(listOf<Pair<UUID, List<Vedtakshistorikkperiode>>>()) { acc, vedtak ->
                    acc + Pair(vedtak.behandlingId, lagTotalbildeForNyttVedtak(vedtak, acc))
                }
                .toMap()
    }

    private fun lagTotalbildeForNyttVedtak(data: BehandlingHistorikkData,
                                           acc: List<Pair<UUID, List<Vedtakshistorikkperiode>>>): List<Vedtakshistorikkperiode> {

        val vedtak = data.vedtakDto
        return when (vedtak) {
            is InnvilgelseOvergangsstønad -> {
                val nyePerioder = vedtak.perioder.map { VedtakshistorikkperiodeOvergangsstønad(it) }
                val førsteFraDato = nyePerioder.first().datoFra
                avkortTidligerePerioder(acc.lastOrNull(), førsteFraDato) + nyePerioder
            }
            is InnvilgelseBarnetilsyn -> {
                val perioder = data.tilkjentYtelse.tilBeløpsperiodeBarnetilsyn(vedtak)
                        .map { VedtakshistorikkperiodeBarnetilsyn(it, data.aktivitetArbeid) }
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
                logger.error("Håndterer ikke ${vedtak::class.java.simpleName} behandling=${data.behandlingId}")
                emptyList()
            }
        }
    }

    private fun splitOppPerioderSomErSanksjonert(acc: List<Pair<UUID, List<Vedtakshistorikkperiode>>>,
                                                 vedtak: Sanksjonert): List<Vedtakshistorikkperiode> {
        val sanksjonsperiode = vedtak.periode.tilPeriode()
        return acc.last().second.flatMap {
            if (!sanksjonsperiode.overlapper(Periode(it.datoFra, it.datoTil))) {
                return@flatMap listOf(it, lagSanksjonertPeriode(it, vedtak))
            }
            val nyePerioder = mutableListOf<Vedtakshistorikkperiode>()
            if (sanksjonsperiode.fradato <= it.datoFra && sanksjonsperiode.tildato < it.datoTil) {
                nyePerioder.add(lagSanksjonertPeriode(it, vedtak))
                nyePerioder.add(it.medFra(datoFra = sanksjonsperiode.tildato.plusDays(1)))
            } else if (sanksjonsperiode.fradato > it.datoFra) {
                nyePerioder.add(it.medTil(datoTil = sanksjonsperiode.fradato.minusDays(1)))
                nyePerioder.add(lagSanksjonertPeriode(it, vedtak))
                if (sanksjonsperiode.tildato < it.datoTil) {
                    nyePerioder.add(it.medFra(datoFra = sanksjonsperiode.tildato.plusDays(1)))
                }
            }
            nyePerioder
        }
    }

    private fun lagSanksjonertPeriode(vedtakshistorikkperiode: Vedtakshistorikkperiode, vedtak: Sanksjonert) =
            when (vedtakshistorikkperiode) {
                is VedtakshistorikkperiodeOvergangsstønad ->
                    VedtakshistorikkperiodeOvergangsstønad(datoFra = vedtak.periode.datoFra(),
                                                           datoTil = vedtak.periode.datoTil(),
                                                           aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT,
                                                           periodeType = VedtaksperiodeType.SANKSJON)
                is VedtakshistorikkperiodeBarnetilsyn ->
                    VedtakshistorikkperiodeBarnetilsyn(
                            datoFra = vedtak.periode.datoFra(),
                            datoTil = vedtak.periode.datoTil(),
                            kontantstøtte = 0,
                            tilleggsstønad = 0,
                            utgifter = BigDecimal.ZERO,
                            antallBarn = 0,
                            aktivitetArbeid = null,
                            erSanksjon = true,
                            barn = emptyList(),
                            sats = 0, //TODO: hva skal vi sette her?
                            beløpFørSatsjustering = 0,
                            )
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

}