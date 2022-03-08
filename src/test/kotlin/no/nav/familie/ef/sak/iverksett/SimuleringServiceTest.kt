package no.nav.familie.ef.sak.iverksett

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.simulering.BlankettSimuleringsService
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.simulering.Simuleringsresultat
import no.nav.familie.ef.sak.simulering.SimuleringsresultatRepository
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.SamordningsfradragType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.kontrakter.ef.iverksett.SimuleringDto
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.LocalDate
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
    private val tilgangService = mockk<TilgangService>()

    private val simuleringService = SimuleringService(iverksettClient = iverksettClient,
                                                      vedtakService = vedtakService,
                                                      blankettSimuleringsService = blankettSimuleringsService,
                                                      simuleringsresultatRepository = simuleringsresultatRepository,
                                                      tilkjentYtelseService = tilkjentYtelseService,
                                                      tilgangService = tilgangService)


    private val personIdent = "12345678901"
    private val fagsak = fagsak(fagsakpersoner(setOf(personIdent)), Stønadstype.OVERGANGSSTØNAD)

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak
        every { tilgangService.validerHarSaksbehandlerrolle() } just Runs
        every { tilgangService.harTilgangTilRolle(any()) } returns true
    }

    @Test
    internal fun `skal bruke lagret tilkjentYtelse for simulering`() {
        val forrigeBehandlingId = behandling(fagsak).id
        val behandling = behandling(fagsak = fagsak,
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    forrigeBehandlingId = forrigeBehandlingId)

        val tilkjentYtelse = tilkjentYtelse(behandlingId = behandling.id, personIdent = personIdent)
        val simuleringsresultat = Simuleringsresultat(behandlingId = behandling.id,
                                                      data = DetaljertSimuleringResultat(emptyList()),
                                                      beriketData = BeriketSimuleringsresultat(mockk(), mockk()))
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { simuleringsresultatRepository.deleteById(any()) } just Runs
        every { simuleringsresultatRepository.insert(any()) } returns simuleringsresultat

        val simulerSlot = slot<SimuleringDto>()
        every {
            iverksettClient.simuler(capture(simulerSlot))
        } returns BeriketSimuleringsresultat(mockk(), mockk())
        simuleringService.simuler(saksbehandling(fagsak, behandling))

        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.behandlingId).isEqualTo(tilkjentYtelse.behandlingId)
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().beløp)
                .isEqualTo(tilkjentYtelse.andelerTilkjentYtelse.first().beløp)
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().fraOgMed)
                .isEqualTo(tilkjentYtelse.andelerTilkjentYtelse.first().stønadFom)
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().tilOgMed)
                .isEqualTo(tilkjentYtelse.andelerTilkjentYtelse.first().stønadTom)
        assertThat(simulerSlot.captured.forrigeBehandlingId).isEqualTo(forrigeBehandlingId)
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
                                                          samordningsfradrag = BigDecimal(300))),
                               samordningsfradragType = SamordningsfradragType.UFØRETRYGD

        )

        every { behandlingService.hentBehandling(any()) } returns behandling


        every {
            vedtakService.hentVedtakHvisEksisterer(any())
        } returns vedtak

        val simulerSlot = slot<SimuleringDto>()
        every {
            iverksettClient.simuler(capture(simulerSlot))
        } returns BeriketSimuleringsresultat(mockk(), mockk())

        simuleringService.simuler(saksbehandling(fagsak, behandling))

        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().fraOgMed)
                .isEqualTo(årMånedFraStart.atDay(1))
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().tilOgMed)
                .isEqualTo(årMånedGEndring.atDay(1).minusDays(1))
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.first().beløp)
                .isGreaterThan(0)

        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.last().fraOgMed)
                .isEqualTo(årMånedGEndring.atDay(1))
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.last().tilOgMed)
                .isEqualTo(årMånedFraSlutt.atEndOfMonth())
        assertThat(simulerSlot.captured.nyTilkjentYtelseMedMetaData.tilkjentYtelse.andelerTilkjentYtelse.last().beløp)
                .isGreaterThan(0)

    }

    @Test
    internal fun `skal feile hvis behandlingen ikke er redigerbar og mangler lagret simulering`() {
        val behandling =
                behandling(fagsak = fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.FATTER_VEDTAK)
        every { behandlingService.hentBehandling(any()) } returns behandling
        assertThrows<RuntimeException> {
            simuleringService.simuler(saksbehandling(id = behandling.id))
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
                                      data = DetaljertSimuleringResultat(emptyList()),
                                      beriketData = BeriketSimuleringsresultat(mockk(), mockk()))
        val simuleringsresultatDto = simuleringService.simuler(saksbehandling(fagsak, behandling))
        assertThat(simuleringsresultatDto).isNotNull
    }

    @Test
    internal fun `skal berike simlueringsresultat`() {
        val forrigeBehandlingId = behandling(fagsak).id
        val behandling = behandling(fagsak = fagsak,
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    forrigeBehandlingId = forrigeBehandlingId)

        val tilkjentYtelse = tilkjentYtelse(behandlingId = behandling.id, personIdent = personIdent)

        every { iverksettClient.simuler(any()) } returns
                objectMapper.readValue(readFile("simuleringsresultat_beriket.json"))

        every { behandlingService.hentBehandling(any()) } returns behandling
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { simuleringsresultatRepository.deleteById(any()) } just Runs

        val simulerSlot = slot<Simuleringsresultat>()
        every { simuleringsresultatRepository.insert(capture(simulerSlot)) } answers { firstArg() }

        simuleringService.simuler(saksbehandling(id = behandling.id))

        assertThat(simulerSlot.captured.beriketData.oppsummering.fom)
                .isEqualTo(LocalDate.of(2021, 2, 1))
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/json/$filnavn").readText()
    }

}