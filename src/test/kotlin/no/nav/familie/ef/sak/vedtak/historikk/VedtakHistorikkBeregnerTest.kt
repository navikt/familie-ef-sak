package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class VedtakHistorikkBeregnerTest {

    private val førsteFra = YearMonth.of(2021, 1)
    private val førsteTil = YearMonth.of(2021, 3)

    private val førstePeriode = lagVedtaksperiode(førsteFra, førsteTil)
    private val førsteVedtak = lagVedtak(perioder = listOf(førstePeriode))

    @Test
    internal fun `opphør har ikke periodeWrapper inne på vedtak`() {
        val andreVedtak = lagVedtak(perioder = null, opphørFom = YearMonth.of(2021, 2))

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))

        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        validerPeriode(
            vedtaksperioderPerBehandling,
            andreVedtak.behandlingId,
            listOf(førstePeriode.copy(periode = førstePeriode.periode.copy(tom = YearMonth.of(2021, 1))).tilHistorikk())
        )
    }

    @Test
    internal fun `vedtak uten revurdering skal ikke endre noe på historikken`() {
        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak))
        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
    }

    @Test
    internal fun `revurdering frem i tiden skal kun legge på tidligere perioder`() {
        val andreVedtak = lagVedtak(perioder = listOf(lagVedtaksperiode(YearMonth.of(2021, 4), YearMonth.of(2021, 4))))

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))

        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        assertThat(vedtaksperioderPerBehandling.getValue(andreVedtak.behandlingId)).isEqualTo(førsteVedtak.vedtaksperioder() + andreVedtak.vedtaksperioder())
    }

    @Test
    internal fun `revurdering bak i tiden skal overskreve alle tidligere perioder`() {
        val andreVedtak = lagVedtak(perioder = listOf(lagVedtaksperiode(YearMonth.of(2020, 4), YearMonth.of(2020, 4))))

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))

        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        validerPeriode(vedtaksperioderPerBehandling, andreVedtak.behandlingId, andreVedtak.vedtaksperioder())
    }

    @Test
    internal fun `revurdering fra samme startdato overskreve alle tidligere perioder`() {
        val andreVedtak =
            lagVedtak(perioder = listOf(førstePeriode.copy(aktivitet = AktivitetType.BARNET_SÆRLIG_TILSYNSKREVENDE)))

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))

        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        assertThat(vedtaksperioderPerBehandling.getValue(andreVedtak.behandlingId)).isNotEqualTo(førsteVedtak.vedtaksperioder())
        validerPeriode(vedtaksperioderPerBehandling, andreVedtak.behandlingId, andreVedtak.vedtaksperioder())
    }

    @Test
    internal fun `revurdering før tidligere perioder skal overskreve alle tidligere perioder`() {
        val andreVedtak = lagVedtak(perioder = listOf(lagVedtaksperiode(YearMonth.of(2020, 4), YearMonth.of(2020, 4))))

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))

        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        validerPeriode(vedtaksperioderPerBehandling, andreVedtak.behandlingId, andreVedtak.vedtaksperioder())
    }

    @Test
    internal fun `revurdering midt i tidligere periode skal overskreve overlappende perioder`() {
        val andreVedtak = lagVedtak(perioder = listOf(lagVedtaksperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 4))))

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))

        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        validerPeriode(
            vedtaksperioderPerBehandling,
            andreVedtak.behandlingId,
            listOf(
                førstePeriode.copy(periode = førstePeriode.periode.copy(tom = YearMonth.of(2021, 1))).tilHistorikk()
            ) + andreVedtak.vedtaksperioder()
        )
    }

    private fun validerFørsteVedtakErUendret(vedtaksperioderPerBehandling: Map<UUID, List<Vedtakshistorikkperiode>>) {
        validerPeriode(vedtaksperioderPerBehandling, førsteVedtak.behandlingId, førsteVedtak.vedtaksperioder())
    }

    private fun validerPeriode(
        vedtaksperioderPerBehandling: Map<UUID, List<Vedtakshistorikkperiode>>,
        behandlingId: UUID,
        vedtaksperioder: List<Vedtakshistorikkperiode>
    ) {
        assertThat(vedtaksperioderPerBehandling.getValue(behandlingId)).isEqualTo(vedtaksperioder)
    }

    private fun Vedtak.vedtaksperioder(): List<Vedtakshistorikkperiode> = this.perioder!!.perioder.map { it.tilHistorikk() }

    private fun lagVedtaksperioderPerBehandling(vedtak: List<Vedtak>): Map<UUID, List<Vedtakshistorikkperiode>> {
        var datoCount = 0L
        val tilkjenteytelser = vedtak.associate {
            val tilkjentYtelse = lagTilkjentYtelse(emptyList(), behandlingId = it.behandlingId)
            val opprettetTid = LocalDate.of(2021, 1, 1).atStartOfDay().plusDays(datoCount++)
            it.behandlingId to tilkjentYtelse.copy(sporbar = Sporbar(opprettetTid = opprettetTid))
        }
        val behandlingHistorikkData = vedtak.map {
            BehandlingHistorikkData(
                it.behandlingId,
                it.tilVedtakDto(),
                null,
                tilkjenteytelser.getValue(it.behandlingId)
            )
        }
        return VedtakHistorikkBeregner.lagVedtaksperioderPerBehandling(behandlingHistorikkData)
    }

    private fun lagVedtaksperiode(fra: YearMonth, til: YearMonth): Vedtaksperiode =
        Vedtaksperiode(
            periode = Månedsperiode(fra, til),
            aktivitet = AktivitetType.BARNET_ER_SYKT,
            periodeType = VedtaksperiodeType.PERIODE_FØR_FØDSEL
        )

    private fun Vedtaksperiode.tilHistorikk() = VedtakshistorikkperiodeOvergangsstønad(
        this.periode,
        sanksjonsårsak = null,
        this.aktivitet,
        this.periodeType
    )

    private fun lagVedtak(
        behandlingId: UUID = UUID.randomUUID(),
        perioder: List<Vedtaksperiode>?,
        opphørFom: YearMonth? = null
    ): Vedtak {
        require((perioder == null) xor (opphørFom == null)) { "Må definiere perioder eller opphørFom" }
        return Vedtak(
            behandlingId = behandlingId,
            resultatType = if (opphørFom == null) ResultatType.INNVILGE else ResultatType.OPPHØRT,
            periodeBegrunnelse = null,
            inntektBegrunnelse = null,
            avslåBegrunnelse = null,
            perioder = perioder?.let { PeriodeWrapper(it.toList()) },
            inntekter = perioder?.let {
                InntektWrapper(
                    listOfNotNull(
                        it.firstOrNull()
                            ?.let {
                                Inntektsperiode(
                                    it.periode,
                                    BigDecimal.ZERO,
                                    BigDecimal.ZERO
                                )
                            }
                    )
                )
            },
            saksbehandlerIdent = null,
            opphørFom = opphørFom,
            beslutterIdent = null
        )
    }
}
