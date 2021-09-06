package no.nav.familie.ef.sak.iverksett

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
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
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.simulering.BlankettSimuleringsService
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.simulering.Simuleringsresultat
import no.nav.familie.ef.sak.simulering.SimuleringsresultatRepository
import no.nav.familie.kontrakter.ef.iverksett.SimuleringDto
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.YearMonth

internal class SimuleringServiceTest {

    private val iverksettClient = mockk<IverksettClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val vedtakService = mockk<VedtakService>()
    private val simuleringsresultatRepository = mockk<SimuleringsresultatRepository>()
    private val beregningService = BeregningService()
    private val blankettSimuleringsService = BlankettSimuleringsService(beregningService)
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()

    private val simuleringService = SimuleringService(iverksettClient = iverksettClient,
                                                      behandlingService = behandlingService,
                                                      fagsakService = fagsakService,
                                                      vedtakService = vedtakService,
                                                      blankettSimuleringsService = blankettSimuleringsService,
                                                      simuleringsresultatRepository = simuleringsresultatRepository,
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
        val simuleringsresultat = Simuleringsresultat(behandlingId = behandling.id,
                                                      data = DetaljertSimuleringResultat(emptyList()))
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling.id
        every { simuleringsresultatRepository.deleteById(any()) } just Runs
        every { simuleringsresultatRepository.insert(any()) } returns simuleringsresultat

        val simulerSlot = slot<SimuleringDto>()
        every {
            iverksettClient.simuler(capture(simulerSlot))
        } returns DetaljertSimuleringResultat(simuleringMottaker = emptyList())
        simuleringService.simuler(behandling.id)

        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.behandlingId).isEqualTo(tilkjentYtelse.behandlingId)
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().beløp).isEqualTo(
                tilkjentYtelse.andelerTilkjentYtelse.first().beløp)
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().fraOgMed).isEqualTo(
                tilkjentYtelse.andelerTilkjentYtelse.first().stønadFom)
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().tilOgMed).isEqualTo(
                tilkjentYtelse.andelerTilkjentYtelse.first().stønadTom)
        assertThat(simulerSlot.captured.forrigeBehandlingId).isEqualTo(behandling.id)
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

        simuleringService.simuler(behandling.id)

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

    @Test
    internal fun `skal feile hvis behandlingen ikke er redigerbar og mangler lagret simulering`() {
        val behandling =
                behandling(fagsak = fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.FATTER_VEDTAK)
        every { behandlingService.hentBehandling(any()) } returns behandling
        assertThrows<RuntimeException> {
            simuleringService.simuler(behandling.id)
        }
    }

    @Test
    internal fun `skal hente lagret simulering hvis behandlingen ikke er redigerbar`() {
        val behandling =
                behandling(fagsak = fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.FATTER_VEDTAK)
        every { behandlingService.hentBehandling(any()) } returns behandling
        every {
            simuleringsresultatRepository.findByIdOrNull(behandling.id)
        } returns Simuleringsresultat(behandlingId = behandling.id,
                                      data = DetaljertSimuleringResultat(emptyList()))
        val simuleringsresultatDto = simuleringService.simuler(behandling.id)
        assertThat(simuleringsresultatDto).isNotNull
    }
}