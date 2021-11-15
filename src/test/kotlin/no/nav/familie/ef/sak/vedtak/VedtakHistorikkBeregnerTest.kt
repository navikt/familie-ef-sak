package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class VedtakHistorikkBeregnerTest {

    private val førsteFra = LocalDate.of(2021, 1, 1)
    private val førsteTil = LocalDate.of(2021, 3, 31)

    private val førstePeriode = lagVedtaksperiode(førsteFra, førsteTil)
    private val førsteVedtak = lagVedtak(perioder = listOf(førstePeriode))

    @Test
    internal fun `vedtak uten revurdering skal ikke endre noe på historikken`() {
        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak))
        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
    }

    @Test
    internal fun `revurdering frem i tiden skal kun legge på tidligere perioder`() {
        val andreVedtak = lagVedtak(perioder = listOf(lagVedtaksperiode(LocalDate.of(2021, 4, 1), LocalDate.of(2021, 4, 30))))

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))

        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        assertThat(vedtaksperioderPerBehandling.getValue(andreVedtak.behandlingId)).isEqualTo(førsteVedtak.vedtaksperioder() + andreVedtak.vedtaksperioder())
    }

    @Test
    internal fun `revurdering bak i tiden skal overskreve alle tidligere perioder`() {
        val andreVedtak = lagVedtak(perioder = listOf(lagVedtaksperiode(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30))))

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
        val andreVedtak = lagVedtak(perioder = listOf(lagVedtaksperiode(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 30))))

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))

        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        validerPeriode(vedtaksperioderPerBehandling, andreVedtak.behandlingId, andreVedtak.vedtaksperioder())
    }

    @Test
    internal fun `revurdering midt i tidligere periode skal overskreve overlappende perioder`() {
        val andreVedtak = lagVedtak(perioder = listOf(lagVedtaksperiode(LocalDate.of(2021, 2, 1), LocalDate.of(2021, 4, 30))))

        val vedtaksperioderPerBehandling = lagVedtaksperioderPerBehandling(listOf(førsteVedtak, andreVedtak))

        validerFørsteVedtakErUendret(vedtaksperioderPerBehandling)
        validerPeriode(vedtaksperioderPerBehandling, andreVedtak.behandlingId,
                       listOf(førstePeriode.copy(datoTil = LocalDate.of(2021, 1, 31))) + andreVedtak.vedtaksperioder())
    }

    private fun validerFørsteVedtakErUendret(vedtaksperioderPerBehandling: Map<UUID, List<Vedtaksperiode>>) {
        validerPeriode(vedtaksperioderPerBehandling, førsteVedtak.behandlingId, førsteVedtak.vedtaksperioder())
    }

    private fun validerPeriode(vedtaksperioderPerBehandling: Map<UUID, List<Vedtaksperiode>>,
                               behandlingId: UUID,
                               vedtaksperioder: List<Vedtaksperiode>) {
        assertThat(vedtaksperioderPerBehandling.getValue(behandlingId)).isEqualTo(vedtaksperioder)
    }

    private fun Vedtak.vedtaksperioder(): List<Vedtaksperiode> = this.perioder!!.perioder

    private fun lagVedtaksperioderPerBehandling(vedtak: List<Vedtak>): Map<UUID, List<Vedtaksperiode>> {
        var datoCount = 0L
        val behandlingPerDato = vedtak.associate {
            it.behandlingId to LocalDate.of(2021, 1, 1).atStartOfDay().plusDays(datoCount++)
        }
        return VedtakHistorikkBeregner.lagVedtaksperioderPerBehandling(vedtak, behandlingPerDato)
    }

    private fun lagVedtaksperiode(fra: LocalDate, til: LocalDate): Vedtaksperiode =
            Vedtaksperiode(datoFra = fra,
                           datoTil = til,
                           aktivitet = AktivitetType.BARNET_ER_SYKT,
                           periodeType = VedtaksperiodeType.PERIODE_FØR_FØDSEL)

    private fun lagVedtak(behandlingId: UUID = UUID.randomUUID(), perioder: List<Vedtaksperiode>) =
            Vedtak(behandlingId = behandlingId,
                   resultatType = ResultatType.INNVILGE,
                   periodeBegrunnelse = null,
                   inntektBegrunnelse = null,
                   avslåBegrunnelse = null,
                   perioder = PeriodeWrapper(perioder.toList()),
                   inntekter = null,
                   saksbehandlerIdent = null,
                   opphørFom = null,
                   beslutterIdent = null)
}