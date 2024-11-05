package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.felles.util.YEAR_MONTH_MAX
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.tilkjentytelse.tilBeløpsperiodeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
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
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class Vedtaksdata(
    val vedtakstidspunkt: LocalDateTime,
    val perioder: List<Vedtakshistorikkperiode>,
)

sealed class Vedtakshistorikkperiode {
    abstract val periode: Månedsperiode

    abstract fun medFra(fra: YearMonth): Vedtakshistorikkperiode

    abstract fun medTil(til: YearMonth): Vedtakshistorikkperiode
}

data class Sanksjonsperiode(
    override val periode: Månedsperiode,
    val sanksjonsårsak: Sanksjonsårsak,
) : Vedtakshistorikkperiode() {
    override fun medFra(fra: YearMonth): Vedtakshistorikkperiode {
        error("Kan ikke endre fra-dato på opphør")
    }

    override fun medTil(til: YearMonth): Vedtakshistorikkperiode {
        error("Kan ikke endre til-dato på opphør")
    }
}

data class Opphørsperiode(
    override val periode: Månedsperiode,
) : Vedtakshistorikkperiode() {
    override fun medFra(fra: YearMonth): Vedtakshistorikkperiode {
        error("Kan ikke endre fra-dato på opphør")
    }

    override fun medTil(til: YearMonth): Vedtakshistorikkperiode {
        error("Kan ikke endre til-dato på opphør")
    }
}

data class VedtakshistorikkperiodeOvergangsstønad(
    override val periode: Månedsperiode,
    val aktivitet: AktivitetType,
    val periodeType: VedtaksperiodeType,
    val inntekt: Inntekt,
) : Vedtakshistorikkperiode() {
    constructor(periode: Månedsperiode, vedtaksperiode: VedtaksperiodeDto, inntekt: Inntekt) :
        this(
            periode = periode,
            aktivitet = vedtaksperiode.aktivitet,
            periodeType = vedtaksperiode.periodeType,
            inntekt = inntekt,
        )

    override fun medFra(fra: YearMonth): Vedtakshistorikkperiode =
        this.copy(
            periode = this.periode.copy(fom = fra),
            inntekt = this.inntekt.copy(årMånedFra = fra),
        )

    override fun medTil(til: YearMonth): Vedtakshistorikkperiode = this.copy(periode = this.periode.copy(tom = til))
}

data class VedtakshistorikkperiodeBarnetilsyn(
    override val periode: Månedsperiode,
    val kontantstøtte: Int,
    val tilleggsstønad: Int,
    val utgifter: BigDecimal,
    val antallBarn: Int,
    val aktivitetArbeid: SvarId?,
    val barn: List<UUID>,
    val sats: Int,
    val beløpFørFratrekkOgSatsjustering: Int,
    val aktivitetstype: AktivitetstypeBarnetilsyn? = null,
    val periodetype: PeriodetypeBarnetilsyn,
) : Vedtakshistorikkperiode() {
    constructor(periode: BeløpsperiodeBarnetilsynDto, aktivitetArbeid: SvarId?) :
        this(
            periode = periode.periode,
            kontantstøtte = periode.beregningsgrunnlag.kontantstøttebeløp.toInt(),
            tilleggsstønad = periode.beregningsgrunnlag.tilleggsstønadsbeløp.toInt(),
            utgifter = periode.beregningsgrunnlag.utgifter,
            antallBarn = periode.beregningsgrunnlag.antallBarn,
            aktivitetArbeid = aktivitetArbeid,
            barn = periode.beregningsgrunnlag.barn,
            sats = periode.sats,
            beløpFørFratrekkOgSatsjustering = periode.beløpFørFratrekkOgSatsjustering,
            aktivitetstype = periode.aktivitetstype,
            periodetype = periode.periodetype,
        )

    override fun medFra(fra: YearMonth): Vedtakshistorikkperiode = this.copy(periode = this.periode.copy(fom = fra))

    override fun medTil(til: YearMonth): Vedtakshistorikkperiode = this.copy(periode = this.periode.copy(tom = til))
}

object VedtakHistorikkBeregner {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Lager totalbilde av vedtak per behandling
     */
    fun lagVedtaksperioderPerBehandling(
        vedtaksliste: List<BehandlingHistorikkData>,
        konfigurasjon: HistorikkKonfigurasjon,
    ): Map<UUID, Vedtaksdata> =
        vedtaksliste
            .sortedBy { it.tilkjentYtelse.sporbar.opprettetTid }
            .fold(listOf<Pair<UUID, Vedtaksdata>>()) { acc, vedtak ->
                acc +
                    Pair(
                        vedtak.behandlingId,
                        Vedtaksdata(vedtak.vedtakstidspunkt, lagTotalbildeForNyttVedtak(vedtak, acc, konfigurasjon)),
                    )
            }.toMap()

    private fun lagTotalbildeForNyttVedtak(
        data: BehandlingHistorikkData,
        acc: List<Pair<UUID, Vedtaksdata>>,
        konfigurasjon: HistorikkKonfigurasjon,
    ): List<Vedtakshistorikkperiode> {
        val vedtak = data.vedtakDto
        return when (vedtak) {
            is InnvilgelseOvergangsstønad -> {
                val nyePerioder = perioderForOvergangsstønad(vedtak)
                val førsteFomDato = nyePerioder.first().periode.fom
                avkortTidligerePerioder(acc.lastOrNull(), førsteFomDato) + nyePerioder
            }
            is InnvilgelseBarnetilsyn -> {
                val perioder =
                    (perioderFraBeløp(vedtak, data, konfigurasjon) + sanksjonsperioder(vedtak))
                        .sortedBy { it.periode }
                val førsteFomDato = perioder.first().periode.fom
                avkortTidligerePerioder(acc.lastOrNull(), førsteFomDato) + perioder
            }
            is Sanksjonert -> {
                splitOppPerioderSomErSanksjonert(acc, vedtak)
            }
            is Opphør -> {
                val opphørFom = vedtak.opphørFom
                val avkortedePerioder = avkortTidligerePerioder(acc.lastOrNull(), opphørFom)
                avkortedePerioder + Opphørsperiode(Månedsperiode(opphørFom))
            }
            else -> {
                logger.error("Håndterer ikke ${vedtak::class.java.simpleName} behandling=${data.behandlingId}")
                emptyList()
            }
        }
    }

    private fun perioderForOvergangsstønad(vedtak: InnvilgelseOvergangsstønad): List<Vedtakshistorikkperiode> {
        val inntekter = inntektsperioder(vedtak)
        return vedtak.perioder.flatMap {
            when (it.periodeType) {
                VedtaksperiodeType.SANKSJON ->
                    listOf(Sanksjonsperiode(it.periode, it.sanksjonsårsak ?: error("Mangler sanksjonsårsak")))
                VedtaksperiodeType.MIDLERTIDIG_OPPHØR -> {
                    val inntekt = Inntekt(it.periode.fom, BigDecimal.ZERO, BigDecimal.ZERO)
                    listOf(VedtakshistorikkperiodeOvergangsstønad(it.periode, it.aktivitet, it.periodeType, inntekt))
                }
                else -> splittOppVedtaksperioderOgInntekter(inntekter, it)
            }
        }
    }

    private fun splittOppVedtaksperioderOgInntekter(
        inntekter: List<Pair<Månedsperiode, Inntekt>>,
        vedtaksperiode: VedtaksperiodeDto,
    ): List<VedtakshistorikkperiodeOvergangsstønad> {
        val overlappendeInntekter = inntekter.filter { inntekt -> inntekt.first.overlapper(vedtaksperiode.periode) }
        feilHvis(overlappendeInntekter.isEmpty()) {
            "Forventer å inneholde minimum en inntektsperiode som overlapper"
        }
        return overlappendeInntekter.map { inntekt ->
            val inntektsperiode = inntekt.first
            val periode =
                Månedsperiode(
                    maxOf(inntektsperiode.fom, vedtaksperiode.periode.fom),
                    minOf(inntektsperiode.tom, vedtaksperiode.periode.tom),
                )
            VedtakshistorikkperiodeOvergangsstønad(periode, vedtaksperiode, inntekt.second)
        }
    }

    private fun inntektsperioder(vedtak: InnvilgelseOvergangsstønad): List<Pair<Månedsperiode, Inntekt>> =
        vedtak.inntekter.windowed(2, 1, true).map { inntektWindow ->
            val tom = inntektWindow.getOrNull(1)?.årMånedFra?.minusMonths(1) ?: YEAR_MONTH_MAX
            val periode = Månedsperiode(inntektWindow[0].årMånedFra, tom)
            periode to inntektWindow[0]
        }

    private fun sanksjonsperioder(vedtak: InnvilgelseBarnetilsyn): List<Sanksjonsperiode> =
        vedtak.perioder
            .filter { it.periodetype == PeriodetypeBarnetilsyn.SANKSJON_1_MND }
            .map { Sanksjonsperiode(it.periode, it.sanksjonsårsak ?: error("Mangler sanksjonsårsak")) }

    private fun perioderFraBeløp(
        vedtak: InnvilgelseBarnetilsyn,
        data: BehandlingHistorikkData,
        konfigurasjon: HistorikkKonfigurasjon,
    ) = data.tilkjentYtelse
        .tilBeløpsperiodeBarnetilsyn(vedtak, konfigurasjon.brukIkkeVedtatteSatser)
        .map { VedtakshistorikkperiodeBarnetilsyn(it, data.aktivitetArbeid) }

    private fun splitOppPerioderSomErSanksjonert(
        acc: List<Pair<UUID, Vedtaksdata>>,
        vedtak: Sanksjonert,
    ): List<Vedtakshistorikkperiode> {
        val sanksjonsperiode = vedtak.periode.tilPeriode()
        val perioder = acc.last().second.perioder
        return perioder.flatMap {
            if (!sanksjonsperiode.overlapper(it.periode)) {
                return@flatMap listOf(it, lagSanksjonertPeriode(vedtak))
            }
            val nyePerioder = mutableListOf<Vedtakshistorikkperiode>()
            if (sanksjonsperiode.fom <= it.periode.fom && sanksjonsperiode.tom < it.periode.tom) {
                nyePerioder.add(lagSanksjonertPeriode(vedtak))
                nyePerioder.add(it.medFra(fra = sanksjonsperiode.tom.plusMonths(1)))
            } else if (sanksjonsperiode.fomDato > it.periode.fomDato) {
                nyePerioder.add(it.medTil(til = sanksjonsperiode.fom.minusMonths(1)))
                nyePerioder.add(lagSanksjonertPeriode(vedtak))
                if (sanksjonsperiode.tomDato < it.periode.tomDato) {
                    nyePerioder.add(it.medFra(fra = sanksjonsperiode.tom.plusMonths(1)))
                }
            }
            nyePerioder
        }
    }

    private fun lagSanksjonertPeriode(vedtak: Sanksjonert) = Sanksjonsperiode(vedtak.periode.tilPeriode(), vedtak.sanksjonsårsak)

    /**
     * Då ett nytt vedtak splitter tidligere vedtaksperioder,
     * så må vi avkorte tidligere periode, då det nye vedtaket overskrever det seneste
     */
    private fun avkortTidligerePerioder(
        sisteVedtak: Pair<UUID, Vedtaksdata>?,
        datoSomTidligerePeriodeOpphør: YearMonth,
    ): List<Vedtakshistorikkperiode> {
        if (sisteVedtak == null) return emptyList()
        return sisteVedtak.second.perioder.mapNotNull {
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
