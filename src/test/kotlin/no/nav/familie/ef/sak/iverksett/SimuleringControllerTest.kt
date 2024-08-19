package no.nav.familie.ef.sak.no.nav.familie.ef.sak.iverksett

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.simulering.SimuleringController
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.simulering.SimuleringsresultatRepository
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingOppryddingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelseType
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal class SimuleringControllerTest {
    private val tilgangService: TilgangService = mockk()
    private val iverksettClient: IverksettClient = mockk()
    private val simuleringResultatRepository: SimuleringsresultatRepository = mockk()
    private val tilkjentYtelseService: TilkjentYtelseService = mockk()
    private val tilordnetRessursService: TilordnetRessursService = mockk()
    private val tilbakekrevingOppryddingService: TilbakekrevingOppryddingService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val simuleringService: SimuleringService = SimuleringService(iverksettClient, simuleringResultatRepository, tilkjentYtelseService, tilgangService, tilordnetRessursService, tilbakekrevingOppryddingService)
    private val controller: SimuleringController =
        SimuleringController(
            tilgangService,
            behandlingService,
            simuleringService,
        )

    val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun init() {
        val saksbehandling = mockk<Saksbehandling>()
        val tilkjentYtelse = lagTilkjentytelse(behandlingId)
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { saksbehandling.eksternId } returns 0L
        every { saksbehandling.eksternFagsakId } returns 0L
        every { saksbehandling.stønadstype } returns StønadType.OVERGANGSSTØNAD
        every { saksbehandling.forrigeBehandlingId } returns UUID.randomUUID()
        every { behandlingService.hentSaksbehandling(behandlingId) } returns saksbehandling
        every { tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.UPDATE) } returns Unit
        every { simuleringService.simuler(saksbehandling) } returns lagSimuleringoppsummering()
    }

    @Test
    internal fun `simuleringsoppsummering skal være endret hvis feilutbetaling har endret seg fra nedlagret simulering`() {
        val lagretSimuleringoppsummering = lagSimuleringoppsummering()
        val simuleringOppsummering = lagSimuleringoppsummering(feilutbetaling = BigDecimal.ONE)

        every { simuleringService.hentLagretSimmuleringsresultat(any()) } returns
            BeriketSimuleringsresultat(
                DetaljertSimuleringResultat(emptyList()),
                lagretSimuleringoppsummering,
            )
        every { iverksettClient.simuler(any()) } returns
            BeriketSimuleringsresultat(
                DetaljertSimuleringResultat(emptyList()),
                simuleringOppsummering,
            )

        val response: Ressurs<Boolean> = controller.erSimuleringsoppsummeringEndret(behandlingId)
        assertThat(response.data).isTrue()
    }

    @Test
    internal fun `simuleringsoppsummering skal være endret hvis feilutbetaling og etterbetaling har endret seg fra nedlagret simulering`() {
        val lagretSimuleringoppsummering = lagSimuleringoppsummering()
        val simuleringOppsummering = lagSimuleringoppsummering(feilutbetaling = BigDecimal.ONE, etterbetaling = BigDecimal.ONE)

        every { simuleringService.hentLagretSimmuleringsresultat(any()) } returns
            BeriketSimuleringsresultat(
                DetaljertSimuleringResultat(emptyList()),
                lagretSimuleringoppsummering,
            )
        every { iverksettClient.simuler(any()) } returns
            BeriketSimuleringsresultat(
                DetaljertSimuleringResultat(emptyList()),
                simuleringOppsummering,
            )

        val response: Ressurs<Boolean> = controller.erSimuleringsoppsummeringEndret(behandlingId)
        assertThat(response.data).isTrue()
    }

    @Test
    internal fun `simuleringsoppsummering skal ikke være endret hvis feilutbetaling og etterbetaling er uendret`() {
        val lagretSimuleringoppsummering = lagSimuleringoppsummering()
        val simuleringOppsummering = lagSimuleringoppsummering()

        every { simuleringService.hentLagretSimmuleringsresultat(any()) } returns
            BeriketSimuleringsresultat(
                DetaljertSimuleringResultat(emptyList()),
                lagretSimuleringoppsummering,
            )
        every { iverksettClient.simuler(any()) } returns
            BeriketSimuleringsresultat(
                DetaljertSimuleringResultat(emptyList()),
                simuleringOppsummering,
            )

        val response: Ressurs<Boolean> = controller.erSimuleringsoppsummeringEndret(behandlingId)
        assertThat(response.data).isFalse()
    }

    private fun lagTilkjentytelse(behandlingId: UUID) =
        TilkjentYtelse(
            behandlingId = behandlingId,
            personident = "123",
            type = TilkjentYtelseType.FØRSTEGANGSBEHANDLING,
            startdato = LocalDate.of(2021, 1, 1),
            andelerTilkjentYtelse =
                listOf(
                    AndelTilkjentYtelse(
                        15000,
                        LocalDate.of(2021, 1, 1),
                        LocalDate.of(2021, 12, 31),
                        "123",
                        inntekt = 0,
                        inntektsreduksjon = 0,
                        samordningsfradrag = 0,
                        kildeBehandlingId = behandlingId,
                    ),
                ),
        )

    private fun lagSimuleringoppsummering(
        etterbetaling: BigDecimal = BigDecimal.ZERO,
        feilutbetaling: BigDecimal = BigDecimal.ZERO,
    ) =
        Simuleringsoppsummering(
            perioder = listOf(),
            fomDatoNestePeriode = LocalDate.now(),
            etterbetaling = etterbetaling,
            feilutbetaling = feilutbetaling,
            fom = LocalDate.now(),
            tomDatoNestePeriode = LocalDate.now(),
            forfallsdatoNestePeriode = LocalDate.now(),
            tidSimuleringHentet = LocalDate.now(),
            tomSisteUtbetaling = LocalDate.now(),
            sumManuellePosteringer = BigDecimal.ZERO,
        )
}
