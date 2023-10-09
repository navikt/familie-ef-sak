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
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.simulering.Simuleringsresultat
import no.nav.familie.ef.sak.simulering.SimuleringsresultatRepository
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.iverksett.SimuleringDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

internal class SimuleringServiceTest {

    private val iverksettClient = mockk<IverksettClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val simuleringsresultatRepository = mockk<SimuleringsresultatRepository>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val tilgangService = mockk<TilgangService>()
    private val tilordnetRessursService = mockk<TilordnetRessursService>()

    private val simuleringService = SimuleringService(
        iverksettClient = iverksettClient,
        simuleringsresultatRepository = simuleringsresultatRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        tilgangService = tilgangService,
        tilordnetRessursService = tilordnetRessursService,
    )

    private val personIdent = "12345678901"
    private val fagsak = fagsak(fagsakpersoner(setOf(personIdent)), StønadType.OVERGANGSSTØNAD)

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
        val behandling = behandling(
            fagsak = fagsak,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            forrigeBehandlingId = forrigeBehandlingId,
        )

        val tilkjentYtelse = tilkjentYtelse(behandlingId = behandling.id, personIdent = personIdent)
        val simuleringsresultat = Simuleringsresultat(
            behandlingId = behandling.id,
            data = DetaljertSimuleringResultat(emptyList()),
            beriketData = BeriketSimuleringsresultat(mockk(), mockk()),
        )
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { simuleringsresultatRepository.deleteById(any()) } just Runs
        every { simuleringsresultatRepository.insert(any()) } returns simuleringsresultat
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true

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
        } returns Simuleringsresultat(
            behandlingId = behandling.id,
            data = DetaljertSimuleringResultat(emptyList()),
            beriketData = BeriketSimuleringsresultat(mockk(), mockk()),
        )
        val simuleringsresultatDto = simuleringService.simuler(saksbehandling(fagsak, behandling))
        assertThat(simuleringsresultatDto).isNotNull
    }

    @Test
    internal fun `skal berike simlueringsresultat`() {
        val forrigeBehandlingId = behandling(fagsak).id
        val behandling = behandling(
            fagsak = fagsak,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            forrigeBehandlingId = forrigeBehandlingId,
        )

        val tilkjentYtelse = tilkjentYtelse(behandlingId = behandling.id, personIdent = personIdent)

        every { iverksettClient.simuler(any()) } returns
            objectMapper.readValue(readFile("simuleringsresultat_beriket.json"))

        every { behandlingService.hentBehandling(any()) } returns behandling
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse
        every { simuleringsresultatRepository.deleteById(any()) } just Runs
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true

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
