package no.nav.familie.ef.sak.tilbakekreving

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.simulering.Simuleringsresultat
import no.nav.familie.ef.sak.simulering.SimuleringsresultatRepository
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class TilbakekrevingOppryddingServiceTest() {
    val simuleringsresultatRepository = mockk<SimuleringsresultatRepository>()
    val tilbakekrevingRepository = mockk<TilbakekrevingRepository>(relaxed = true)
    val behandlingRepository = mockk<BehandlingRepository>()
    val oppryddingService = TilbakekrevingOppryddingService(tilbakekrevingRepository = tilbakekrevingRepository, simuleringsresultatRepository = simuleringsresultatRepository, behandlingRepository = behandlingRepository)

    val behandling = behandling(fagsak = fagsak())

    val simuleringsoppsummering =
        Simuleringsoppsummering(
            perioder = listOf(),
            fomDatoNestePeriode = null,
            etterbetaling = BigDecimal.valueOf(5000),
            feilutbetaling = BigDecimal.valueOf(40_000),
            fom = LocalDate.of(2021, 1, 1),
            tomDatoNestePeriode = null,
            forfallsdatoNestePeriode = null,
            tidSimuleringHentet = LocalDate.of(2021, 11, 1),
            tomSisteUtbetaling = LocalDate.of(2021, 10, 31),
        )

    @BeforeEach
    fun setUp() {
        mockhentBehandling()
    }

    @Test
    internal fun `Skal slette tilbakekreving dersom det ikke er feilutbetaling i ny simulering`() {
        mockHentLagretSimuleringsresultat(feilutbetaltBeløp = 1234)
        mockHentLagretTilbakekreving(tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_UTEN_VARSEL)
        val nySimuleringsoppsummering = simuleringsoppsummering.copy(feilutbetaling = BigDecimal.ZERO)
        oppryddingService.slettTilbakekrevingsvalgHvisIngenFeilutbetalingEllerForskjelligBeløp(UUID.randomUUID(), nySimuleringsoppsummering)

        verify(exactly = 1) { tilbakekrevingRepository.deleteById(any()) }
    }

    @Test
    internal fun `Skal slette tilbakekreving dersom feilutbetalt beløp er endret og tilbakekrevingsvalg var OPPRETT AUTOMATISK`() {
        mockHentLagretSimuleringsresultat(feilutbetaltBeløp = 1234)
        mockHentLagretTilbakekreving(tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_AUTOMATISK)

        oppryddingService.slettTilbakekrevingsvalgHvisIngenFeilutbetalingEllerForskjelligBeløp(UUID.randomUUID(), simuleringsoppsummering)

        verify(exactly = 1) { tilbakekrevingRepository.deleteById(any()) }
    }

    @Test
    internal fun `Skal ikke slette tilbakekreving dersom tilbakekrevingsvalg ikke er OPPRETT AUTOMATISK`() {
        mockHentLagretSimuleringsresultat(feilutbetaltBeløp = 1234)
        mockHentLagretTilbakekreving(tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_MED_VARSEL)

        oppryddingService.slettTilbakekrevingsvalgHvisIngenFeilutbetalingEllerForskjelligBeløp(UUID.randomUUID(), simuleringsoppsummering)

        verify(exactly = 0) { tilbakekrevingRepository.deleteById(any()) }
    }

    @Test
    internal fun `Skal ikke slette tilbakekreving dersom feilutbetalt beløp ikke er endret og tilbakekrevingsvalg var OPPRETT AUTOMATISK`() {
        mockHentLagretSimuleringsresultat(feilutbetaltBeløp = simuleringsoppsummering.feilutbetaling.toInt())
        mockHentLagretTilbakekreving(tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_AUTOMATISK)

        oppryddingService.slettTilbakekrevingsvalgHvisIngenFeilutbetalingEllerForskjelligBeløp(UUID.randomUUID(), simuleringsoppsummering)

        verify(exactly = 0) { tilbakekrevingRepository.deleteById(any()) }
    }

    private fun mockhentBehandling() {
        every { behandlingRepository.findByIdOrThrow(any()) } returns behandling
    }

    private fun mockHentLagretTilbakekreving(tilbakekrevingsvalg: Tilbakekrevingsvalg) {
        every { tilbakekrevingRepository.findByIdOrThrow(any()) } returns
            Tilbakekreving(
                behandlingId = UUID.randomUUID(),
                valg = tilbakekrevingsvalg,
                varseltekst = "forventetVarseltekst",
                begrunnelse = "ingen",
            )
    }

    private fun mockHentLagretSimuleringsresultat(feilutbetaltBeløp: Int) {
        every { simuleringsresultatRepository.findByIdOrNull(any()) }
            .returns(
                Simuleringsresultat(
                    behandlingId = UUID.randomUUID(),
                    data = DetaljertSimuleringResultat(emptyList()),
                    beriketData =
                        BeriketSimuleringsresultat(
                            mockk(),
                            simuleringsoppsummering.copy(feilutbetaling = BigDecimal(feilutbetaltBeløp)),
                        ),
                ),
            )
    }
}
