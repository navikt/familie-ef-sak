package no.nav.familie.ef.sak.iverksett

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.api.beregning.BeregningService
import no.nav.familie.ef.sak.api.beregning.Inntekt
import no.nav.familie.ef.sak.api.beregning.Innvilget
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.api.beregning.VedtaksperiodeDto
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.AktivitetType
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

internal class SimuleringServiceTest {

    private val iverksettClient = mockk<IverksettClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val vedtakService = mockk<VedtakService>()
    private val beregningService = BeregningService()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()

    private val simuleringService = SimuleringService(iverksettClient = iverksettClient,
                                                      behandlingService = behandlingService,
                                                      fagsakService = fagsakService,
                                                      vedtakService = vedtakService,
                                                      beregningService = beregningService,
                                                      tilkjentYtelseService = tilkjentYtelseService)


    private val personIdent = "12345678901"
    private val fagsak = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.OVERGANGSSTØNAD)

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
    }

    @Test
    internal fun `skal bruke lagret tilkjentYtelse for simulering`() {

        val behandling = behandling(fagsak = fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING)

        val tilkjentYtelse = tilkjentYtelse(behandlingId = behandling.id, personIdent = personIdent)
        every { behandlingService.hentBehandling(any()) } returns behandling
        every {
            tilkjentYtelseService.hentForBehandling(any())
        } returns tilkjentYtelse
        every {
            tilkjentYtelseService.finnSisteTilkjentYtelse(any())
        } returns null

        val simulerSlot = slot<SimuleringDto>()
        every {
            iverksettClient.simuler(capture(simulerSlot))
        } returns DetaljertSimuleringResultat(simuleringMottaker = emptyList())
        simuleringService.simulerForBehandling(behandling.id)

        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.behandlingId).isEqualTo(tilkjentYtelse.behandlingId)
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().beløp).isEqualTo(
                tilkjentYtelse.andelerTilkjentYtelse.first().beløp)
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().fraOgMed).isEqualTo(
                tilkjentYtelse.andelerTilkjentYtelse.first().stønadFom)
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().tilOgMed).isEqualTo(
                tilkjentYtelse.andelerTilkjentYtelse.first().stønadTom)

    }

    @Test
    internal fun `skal bruke lagret vedtak for simulering av blankett`() {

        val behandling = behandling(fagsak = fagsak, type = BehandlingType.BLANKETT)

        val årMånedFraStart = YearMonth.of(2021, 1)
        val årMånedGEndring = YearMonth.of(2021, 5)
        val årMånedFraSlutt = YearMonth.of(2021, 12)
        val vedtak = Innvilget(resultatType = ResultatType.INNVILGE,
                               periodeBegrunnelse = "Ok",
                               inntektBegrunnelse = "ok",
                               perioder = listOf(VedtaksperiodeDto(årMånedFra = årMånedFraStart,
                                                                   årMånedTil = årMånedFraSlutt,
                                                                   aktivitet = AktivitetType.BARN_UNDER_ETT_ÅR,
                                                                   periodeType = VedtaksperiodeType.HOVEDPERIODE)),
                               inntekter = listOf(Inntekt(årMånedFra = årMånedFraStart,
                                                          forventetInntekt = BigDecimal(300000),
                                                          samordningsfradrag = BigDecimal(300)))

        )

        every { behandlingService.hentBehandling(any()) } returns behandling


        every {
            vedtakService.hentVedtakHvisEksisterer(any())
        } returns vedtak

        val simulerSlot = slot<SimuleringDto>()
        every {
            iverksettClient.simuler(capture(simulerSlot))
        } returns DetaljertSimuleringResultat(simuleringMottaker = emptyList())
        simuleringService.simulerForBehandling(behandling.id)

        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().fraOgMed).isEqualTo(
                årMånedFraStart.atDay(1))
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().tilOgMed).isEqualTo(
                årMånedGEndring.atDay(1).minusDays(1))
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().beløp).isGreaterThan(
                0)

        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.last().fraOgMed).isEqualTo(
                årMånedGEndring.atDay(1))
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.last().tilOgMed).isEqualTo(
                årMånedFraSlutt.atEndOfMonth())
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.last().beløp).isGreaterThan(
                0)

    }


}