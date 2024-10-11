package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.beregning.Inntekt
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class VedtakHistorikkBeregnerTest {
    private val førsteFra = LocalDate.of(2021, 1, 1)
    private val førsteTil = LocalDate.of(2021, 3, 31)

    private val førstePeriode = lagVedtaksperiode(førsteFra, førsteTil)
    private val førsteVedtak = lagVedtak(perioder = listOf(førstePeriode))

    @Test
    internal fun `opphør avkorter tidligere vedtak og lager egen opphørsperiode`() {
        val opphørFom = YearMonth.of(2021, 2)
        val andreVedtak = lagVedtak(perioder = null, opphørFom = opphørFom)

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))
        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        validerPeriode(
            vedtaksperioderPerBehandling,
            andreVedtak.behandlingId,
            listOf(
                førstePeriode.copy(datoTil = LocalDate.of(2021, 1, 31)).tilHistorikk(),
                Opphørsperiode(Månedsperiode(opphørFom)),
            ),
        )
    }

    @Test
    internal fun `vedtak uten revurdering skal ikke endre noe på historikken`() {
        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak))
        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
    }

    @Test
    internal fun `revurdering frem i tiden skal kun legge på tidligere perioder`() {
        val andreVedtak =
            lagVedtak(perioder = listOf(lagVedtaksperiode(LocalDate.of(2021, 4, 1), LocalDate.of(2021, 4, 30))))

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))

        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        assertThat(vedtaksperioderPerBehandling.getValue(andreVedtak.behandlingId)).isEqualTo(førsteVedtak.vedtaksperioder() + andreVedtak.vedtaksperioder())
    }

    @Test
    internal fun `revurdering bak i tiden skal overskreve alle tidligere perioder`() {
        val andreVedtak =
            lagVedtak(perioder = listOf(lagVedtaksperiode(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30))))

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
        val andreVedtak =
            lagVedtak(perioder = listOf(lagVedtaksperiode(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30))))

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))

        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        validerPeriode(vedtaksperioderPerBehandling, andreVedtak.behandlingId, andreVedtak.vedtaksperioder())
    }

    @Test
    internal fun `revurdering midt i tidligere periode skal overskreve overlappende perioder`() {
        val andreVedtak =
            lagVedtak(perioder = listOf(lagVedtaksperiode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 4, 30))))

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))

        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        validerPeriode(
            vedtaksperioderPerBehandling,
            andreVedtak.behandlingId,
            listOf(
                førstePeriode
                    .copy(datoTil = LocalDate.of(2021, 1, 31))
                    .tilHistorikk(),
            ) + andreVedtak.vedtaksperioder(),
        )
    }

    @Nested
    inner class SplitteVedtaksperiodeBasertPåInntekter {
        val januar = YearMonth.of(2021, 1)
        val februar = YearMonth.of(2021, 2)
        val mars = YearMonth.of(2021, 3)
        val april = YearMonth.of(2021, 4)

        @Test
        internal fun `en periode med 2 inntektsperioder`() {
            val vedtak =
                lagVedtak(
                    perioder = listOf(lagVedtaksperiode(januar.atDay(1), mars.atEndOfMonth())),
                    inntektsperioder = listOf(lagInntekt(januar, februar, 10), lagInntekt(mars, april, 5)),
                )

            val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(vedtak))
            val vedtaksperioder = vedtaksperioderPerBehandling.values.toList()
            assertThat(vedtaksperioder).hasSize(1)
            val vedtaksperioderForBehandling =
                vedtaksperioder[0].filterIsInstance<VedtakshistorikkperiodeOvergangsstønad>()
            assertThat(vedtaksperioderForBehandling).hasSize(2)

            assertThat(vedtaksperioderForBehandling[0].periode).isEqualTo(Månedsperiode(januar, februar))
            assertThat(vedtaksperioderForBehandling[0].inntekt.årMånedFra).isEqualTo(januar)
            assertThat(vedtaksperioderForBehandling[0].inntekt.forventetInntekt?.toInt()).isEqualTo(10)

            assertThat(vedtaksperioderForBehandling[1].periode).isEqualTo(Månedsperiode(mars, mars))
            assertThat(vedtaksperioderForBehandling[1].inntekt.årMånedFra).isEqualTo(mars)
            assertThat(vedtaksperioderForBehandling[1].inntekt.forventetInntekt?.toInt()).isEqualTo(5)
        }

        @Test
        internal fun `2 perioder med 1 inntekt`() {
            val vedtak =
                lagVedtak(
                    perioder =
                        listOf(
                            lagVedtaksperiode(januar.atDay(1), mars.atEndOfMonth()),
                            lagVedtaksperiode(april.atDay(1), april.atEndOfMonth()),
                        ),
                    inntektsperioder = listOf(lagInntekt(januar, april, 10)),
                )

            val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(vedtak))
            val vedtaksperioder = vedtaksperioderPerBehandling.values.toList()
            assertThat(vedtaksperioder).hasSize(1)
            val vedtaksperioderForBehandling =
                vedtaksperioder[0].filterIsInstance<VedtakshistorikkperiodeOvergangsstønad>()
            assertThat(vedtaksperioderForBehandling).hasSize(2)

            assertThat(vedtaksperioderForBehandling[0].periode).isEqualTo(Månedsperiode(januar, mars))
            assertThat(vedtaksperioderForBehandling[0].inntekt.årMånedFra).isEqualTo(januar)

            assertThat(vedtaksperioderForBehandling[1].periode).isEqualTo(Månedsperiode(april, april))
            assertThat(vedtaksperioderForBehandling[1].inntekt.årMånedFra).isEqualTo(januar)
        }
    }

    private fun validerFørsteVedtakErUendret(vedtaksperioderPerBehandling: Map<UUID, List<Vedtakshistorikkperiode>>) {
        validerPeriode(vedtaksperioderPerBehandling, førsteVedtak.behandlingId, førsteVedtak.vedtaksperioder())
    }

    private fun validerPeriode(
        vedtaksperioderPerBehandling: Map<UUID, List<Vedtakshistorikkperiode>>,
        behandlingId: UUID,
        vedtaksperioder: List<Vedtakshistorikkperiode>,
    ) {
        assertThat(vedtaksperioderPerBehandling.getValue(behandlingId)).isEqualTo(vedtaksperioder)
    }

    private fun Vedtak.vedtaksperioder(): List<Vedtakshistorikkperiode> =
        this.perioder!!.perioder.map { it.tilHistorikk() }

    private fun lagVedtaksperioderPerBehandling(vedtak: List<Vedtak>): Map<UUID, List<Vedtakshistorikkperiode>> {
        var datoCount = 0L
        val tilkjenteytelser =
            vedtak.associate {
                val tilkjentYtelse = lagTilkjentYtelse(emptyList(), behandlingId = it.behandlingId)
                val opprettetTid = LocalDate.of(2021, 1, 1).atStartOfDay().plusDays(datoCount++)
                it.behandlingId to tilkjentYtelse.copy(sporbar = Sporbar(opprettetTid = opprettetTid))
            }
        val behandlingHistorikkData =
            vedtak.map {
                BehandlingHistorikkData(
                    behandlingId = it.behandlingId,
                    vedtakstidspunkt = LocalDateTime.now(),
                    vedtakDto = it.tilVedtakDto(),
                    aktivitetArbeid = null,
                    tilkjentYtelse = tilkjenteytelser.getValue(it.behandlingId),
                )
            }
        val konfigurasjon = HistorikkKonfigurasjon(true)
        return VedtakHistorikkBeregner
            .lagVedtaksperioderPerBehandling(behandlingHistorikkData, konfigurasjon)
            .map { it.key to it.value.perioder }
            .toMap()
    }

    private fun lagVedtaksperiode(
        fra: LocalDate,
        til: LocalDate,
    ): Vedtaksperiode =
        Vedtaksperiode(
            datoFra = fra,
            datoTil = til,
            aktivitet = AktivitetType.BARNET_ER_SYKT,
            periodeType = VedtaksperiodeType.PERIODE_FØR_FØDSEL,
        )

    private fun Vedtaksperiode.tilHistorikk() =
        VedtakshistorikkperiodeOvergangsstønad(
            Månedsperiode(this.datoFra, this.datoTil),
            this.aktivitet,
            this.periodeType,
            Inntekt(YearMonth.from(this.datoFra), BigDecimal.ZERO, BigDecimal.ZERO),
        )

    private fun lagVedtak(
        behandlingId: UUID = UUID.randomUUID(),
        perioder: List<Vedtaksperiode>?,
        opphørFom: YearMonth? = null,
        inntektsperioder: List<Inntektsperiode>? = null,
    ): Vedtak {
        require((perioder == null) xor (opphørFom == null)) { "Må definiere perioder eller opphørFom" }
        return Vedtak(
            behandlingId = behandlingId,
            resultatType = if (opphørFom == null) ResultatType.INNVILGE else ResultatType.OPPHØRT,
            periodeBegrunnelse = null,
            inntektBegrunnelse = null,
            avslåBegrunnelse = null,
            perioder = perioder?.let { PeriodeWrapper(it.toList()) },
            inntekter = inntektsperioder?.let { InntektWrapper(it) } ?: defaultInntektsperioder(perioder),
            saksbehandlerIdent = null,
            opphørFom = opphørFom,
            beslutterIdent = null,
        )
    }

    private fun lagInntekt(
        fom: YearMonth,
        tom: YearMonth,
        inntekt: Int,
    ) = Inntektsperiode(
        periode = Månedsperiode(fom, tom),
        inntekt = inntekt.toBigDecimal(),
        samordningsfradrag = BigDecimal.ZERO,
    )

    private fun defaultInntektsperioder(perioder: List<Vedtaksperiode>?): InntektWrapper? =
        perioder?.let {
            val inntekt = it.firstOrNull()?.let { lagInntekt(it.periode.fom, it.periode.tom, 0) }
            InntektWrapper(listOfNotNull(inntekt))
        }
}
