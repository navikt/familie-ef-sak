package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.tilkjentytelse.tilBeløpsperiodeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonert
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

sealed class Vedtakshistorikkperiode {

    abstract val periode: Månedsperiode
    abstract val erSanksjon: Boolean
    abstract val sanksjonsårsak: Sanksjonsårsak?

    abstract fun medFra(fra: YearMonth): Vedtakshistorikkperiode
    abstract fun medTil(til: YearMonth): Vedtakshistorikkperiode
}

data class VedtakshistorikkperiodeOvergangsstønad(
    override val periode: Månedsperiode,
    override val sanksjonsårsak: Sanksjonsårsak? = null,
    val aktivitet: AktivitetType,
    val periodeType: VedtaksperiodeType,
) : Vedtakshistorikkperiode() {

    override val erSanksjon = periodeType == VedtaksperiodeType.SANKSJON

    constructor(periode: VedtaksperiodeDto) :
        this(
            periode = periode.periode,
            aktivitet = periode.aktivitet,
            periodeType = periode.periodeType
        )

    override fun medFra(fra: YearMonth): Vedtakshistorikkperiode {
        return this.copy(periode = this.periode.copy(fom = fra))
    }

    override fun medTil(til: YearMonth): Vedtakshistorikkperiode {
        return this.copy(periode = this.periode.copy(tom = til))
    }
}

data class VedtakshistorikkperiodeBarnetilsyn(
    override val periode: Månedsperiode,
    override val erSanksjon: Boolean,
    override val sanksjonsårsak: Sanksjonsårsak? = null,
    val kontantstøtte: Int,
    val tilleggsstønad: Int,
    val utgifter: BigDecimal,
    val antallBarn: Int,
    val aktivitetArbeid: SvarId?,
    val barn: List<UUID>,
    val sats: Int,
    val beløpFørFratrekkOgSatsjustering: Int,
) : Vedtakshistorikkperiode() {

    constructor(periode: BeløpsperiodeBarnetilsynDto, aktivitetArbeid: SvarId?) :
        this(
            periode = periode.periode.toMånedsperiode(),
            erSanksjon = false,
            kontantstøtte = periode.beregningsgrunnlag.kontantstøttebeløp.toInt(),
            tilleggsstønad = periode.beregningsgrunnlag.tilleggsstønadsbeløp.toInt(),
            utgifter = periode.beregningsgrunnlag.utgifter,
            antallBarn = periode.beregningsgrunnlag.antallBarn,
            aktivitetArbeid = aktivitetArbeid,
            barn = periode.beregningsgrunnlag.barn,
            sats = periode.sats,
            beløpFørFratrekkOgSatsjustering = periode.beløpFørFratrekkOgSatsjustering,
        )

    override fun medFra(fra: YearMonth): Vedtakshistorikkperiode {
        return this.copy(periode = this.periode.copy(fom = fra))
    }

    override fun medTil(til: YearMonth): Vedtakshistorikkperiode {
        return this.copy(periode = this.periode.copy(tom = til))
    }
}

object VedtakHistorikkBeregner {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun lagVedtaksperioderPerBehandling(vedtaksliste: List<BehandlingHistorikkData>): Map<UUID, List<Vedtakshistorikkperiode>> {
        return vedtaksliste
            .sortedBy { it.tilkjentYtelse.sporbar.opprettetTid }
            .fold(listOf<Pair<UUID, List<Vedtakshistorikkperiode>>>()) { acc, vedtak ->
                acc + Pair(vedtak.behandlingId, lagTotalbildeForNyttVedtak(vedtak, acc))
            }
            .toMap()
    }

    private fun lagTotalbildeForNyttVedtak(
        data: BehandlingHistorikkData,
        acc: List<Pair<UUID, List<Vedtakshistorikkperiode>>>
    ): List<Vedtakshistorikkperiode> {

        val vedtak = data.vedtakDto
        return when (vedtak) {
            is InnvilgelseOvergangsstønad -> {
                val nyePerioder = vedtak.perioder.map { VedtakshistorikkperiodeOvergangsstønad(it) }
                val førsteFomDato = nyePerioder.first().periode.fom
                avkortTidligerePerioder(acc.lastOrNull(), førsteFomDato) + nyePerioder
            }
            is InnvilgelseBarnetilsyn -> {
                val perioder = data.tilkjentYtelse.tilBeløpsperiodeBarnetilsyn(vedtak)
                    .map { VedtakshistorikkperiodeBarnetilsyn(it, data.aktivitetArbeid) }
                val førsteFomDato = perioder.first().periode.fom
                avkortTidligerePerioder(acc.lastOrNull(), førsteFomDato) + perioder
            }
            is Sanksjonert -> {
                splitOppPerioderSomErSanksjonert(acc, vedtak)
            }
            is Opphør -> {
                val opphørFom = vedtak.opphørFom
                avkortTidligerePerioder(acc.lastOrNull(), opphørFom)
            }
            else -> {
                logger.error("Håndterer ikke ${vedtak::class.java.simpleName} behandling=${data.behandlingId}")
                emptyList()
            }
        }
    }

    private fun splitOppPerioderSomErSanksjonert(
        acc: List<Pair<UUID, List<Vedtakshistorikkperiode>>>,
        vedtak: Sanksjonert
    ): List<Vedtakshistorikkperiode> {
        val sanksjonsperiode = vedtak.periode.tilPeriode()
        return acc.last().second.flatMap {
            if (!sanksjonsperiode.overlapper(it.periode)) {
                return@flatMap listOf(it, lagSanksjonertPeriode(it, vedtak))
            }
            val nyePerioder = mutableListOf<Vedtakshistorikkperiode>()
            if (sanksjonsperiode.fom <= it.periode.fom && sanksjonsperiode.tom < it.periode.tom) {
                nyePerioder.add(lagSanksjonertPeriode(it, vedtak))
                nyePerioder.add(it.medFra(fra = sanksjonsperiode.tom.plusMonths(1)))
            } else if (sanksjonsperiode.fomDato > it.periode.fomDato) {
                nyePerioder.add(it.medTil(til = sanksjonsperiode.fom.minusMonths(1)))
                nyePerioder.add(lagSanksjonertPeriode(it, vedtak))
                if (sanksjonsperiode.tomDato < it.periode.tomDato) {
                    nyePerioder.add(it.medFra(fra = sanksjonsperiode.tom.plusMonths(1)))
                }
            }
            nyePerioder
        }
    }

    private fun lagSanksjonertPeriode(vedtakshistorikkperiode: Vedtakshistorikkperiode, vedtak: Sanksjonert) =
        when (vedtakshistorikkperiode) {
            is VedtakshistorikkperiodeOvergangsstønad ->
                VedtakshistorikkperiodeOvergangsstønad(
                    periode = vedtak.periode.tilPeriode(),
                    aktivitet = AktivitetType.IKKE_AKTIVITETSPLIKT,
                    periodeType = VedtaksperiodeType.SANKSJON,
                    sanksjonsårsak = vedtak.sanksjonsårsak
                )
            is VedtakshistorikkperiodeBarnetilsyn ->
                VedtakshistorikkperiodeBarnetilsyn(
                    periode = vedtak.periode.tilPeriode(),
                    kontantstøtte = 0,
                    tilleggsstønad = 0,
                    utgifter = BigDecimal.ZERO,
                    antallBarn = 0,
                    aktivitetArbeid = null,
                    erSanksjon = true,
                    barn = emptyList(),
                    sats = 0,
                    beløpFørFratrekkOgSatsjustering = 0,
                    sanksjonsårsak = vedtak.sanksjonsårsak,
                )
        }

    /**
     * Då ett nytt vedtak splitter tidligere vedtaksperioder,
     * så må vi avkorte tidligere periode, då det nye vedtaket overskrever det seneste
     */
    private fun avkortTidligerePerioder(
        sisteVedtak: Pair<UUID, List<Vedtakshistorikkperiode>>?,
        datoSomTidligerePeriodeOpphør: YearMonth
    ): List<Vedtakshistorikkperiode> {
        if (sisteVedtak == null) return emptyList()
        return sisteVedtak.second.mapNotNull {
            if (it.periode.fom >= datoSomTidligerePeriodeOpphør) {
                null
            } else if (it.periode.tom < datoSomTidligerePeriodeOpphør) {
                it
            } else {
                it.medTil(til = datoSomTidligerePeriodeOpphør.minusMonths(1))
            }
        }
    }
}
