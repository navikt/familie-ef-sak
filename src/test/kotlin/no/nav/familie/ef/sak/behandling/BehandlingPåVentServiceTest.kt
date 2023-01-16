package no.nav.familie.ef.sak.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.TaAvVentStatus
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.felles.util.mockFeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.vedtak.NullstillVedtakService
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.util.UUID

internal class BehandlingPåVentServiceTest {

    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val taskService = mockk<TaskService>(relaxed = true)
    private val nullstillVedtakService = mockk<NullstillVedtakService>(relaxed = true)
    private val behandlingPåVentService =
        BehandlingPåVentService(
            behandlingService,
            taskService,
            nullstillVedtakService,
            mockFeatureToggleService()
        )
    val fagsak = fagsak()
    val tidligereIverksattBehandling = behandling(fagsak)
    val behandling = behandling(fagsak)
    val behandlingId = behandling.id

    @BeforeEach
    internal fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler(true) } returns "bob"
        mockFinnSisteIverksatteBehandling(null)
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class SettPåVent {

        @Test
        fun `skal sette behandling på vent hvis den kan redigeres og sende melding til DVH`() {
            mockHentBehandling(BehandlingStatus.UTREDES)

            behandlingPåVentService.settPåVent(behandlingId)

            verify { behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.SATT_PÅ_VENT) }
            verify {
                taskService.save(
                    coWithArg {
                        assertThat(it.type).isEqualTo(BehandlingsstatistikkTask.TYPE)
                        assertThat(it.payload).contains(Hendelse.VENTER.name)
                    }
                )
            }
        }

        @Test
        fun `skal ikke sette behandling på vent hvis den er sperret for redigering`() {
            mockHentBehandling(BehandlingStatus.FATTER_VEDTAK)

            val feil: ApiFeil = assertThrows { behandlingPåVentService.settPåVent(behandlingId) }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class KanTaAvVent {

        @BeforeEach
        internal fun setUp() {
            mockHentBehandling(BehandlingStatus.SATT_PÅ_VENT)
        }

        @Test
        internal fun `kan ta av vent når det ikke finnes andre behandlinger`() {
            every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()

            val kanTaAvVent = behandlingPåVentService.kanTaAvVent(behandlingId)
            assertThat(kanTaAvVent.status).isEqualTo(TaAvVentStatus.OK)
            assertThat(kanTaAvVent.nyForrigeBehandlingId).isEqualTo(null)
        }

        @Test
        internal fun `kaster feil når behandlingen ikke har status PÅ_VENT`() {
            mockHentBehandling(BehandlingStatus.UTREDES)

            val feil: ApiFeil = assertThrows { behandlingPåVentService.kanTaAvVent(behandlingId) }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `kan ta av vent hvis siste behandling på vent peker til siste iverksatte behandling`() {
            mockHentBehandling(BehandlingStatus.SATT_PÅ_VENT, forrigeBehandlingId = tidligereIverksattBehandling.id)
            mockFinnSisteIverksatteBehandling(tidligereIverksattBehandling)

            val kanTaAvVent = behandlingPåVentService.kanTaAvVent(behandlingId)
            assertThat(kanTaAvVent.status).isEqualTo(TaAvVentStatus.OK)
            assertThat(kanTaAvVent.nyForrigeBehandlingId).isEqualTo(null)
        }

        @Test
        internal fun `må oppdatere behandling hvis siste iverksatte behandling er en annen enn behandling på vent peker til`() {
            mockHentBehandling(BehandlingStatus.SATT_PÅ_VENT, forrigeBehandlingId = UUID.randomUUID())
            mockFinnSisteIverksatteBehandling(tidligereIverksattBehandling)

            val kanTaAvVent = behandlingPåVentService.kanTaAvVent(behandlingId)
            assertThat(kanTaAvVent.status).isEqualTo(TaAvVentStatus.MÅ_NULSTILLE_VEDTAK)
            assertThat(kanTaAvVent.nyForrigeBehandlingId).isEqualTo(tidligereIverksattBehandling.id)
        }

        @Test
        internal fun `annen behandling må ferdigstilles`() {
            mockHentBehandling(BehandlingStatus.SATT_PÅ_VENT, forrigeBehandlingId = UUID.randomUUID())
            mockHentBehandlinger(behandling(fagsak, status = BehandlingStatus.IVERKSETTER_VEDTAK))

            val kanTaAvVent = behandlingPåVentService.kanTaAvVent(behandlingId)
            assertThat(kanTaAvVent.status).isEqualTo(TaAvVentStatus.ANNEN_BEHANDLING_MÅ_FERDIGSTILLES)
            assertThat(kanTaAvVent.nyForrigeBehandlingId).isEqualTo(null)
        }
    }

    @Nested
    inner class TaAvVent {

        @BeforeEach
        internal fun setUp() {
            mockHentBehandling(BehandlingStatus.SATT_PÅ_VENT)
        }

        @Test
        fun `skal ta behandling av vent og sende melding til DVH`() {
            behandlingPåVentService.taAvVent(behandlingId)

            verify { behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES) }
            verify {
                taskService.save(
                    coWithArg {
                        assertThat(it.type).isEqualTo(BehandlingsstatistikkTask.TYPE)
                        assertThat(it.payload).contains(Hendelse.PÅBEGYNT.name)
                    }
                )
            }
            verify(exactly = 0) { nullstillVedtakService.nullstillVedtak(any()) }
            verify(exactly = 0) { behandlingService.oppdaterForrigeBehandlingId(any(), any()) }
        }

        @Test
        fun `skal feile hvis behandling ikke er på vent`() {
            mockHentBehandling(BehandlingStatus.FATTER_VEDTAK)

            val feil: ApiFeil = assertThrows { behandlingPåVentService.taAvVent(behandlingId) }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `skal feile hvis det finnes en annen behandling som må ferdigstilles`() {
            mockHentBehandlinger(behandling(fagsak, status = BehandlingStatus.IVERKSETTER_VEDTAK))

            val feil: ApiFeil = assertThrows { behandlingPåVentService.taAvVent(behandlingId) }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `skal oppdatere nullstille vedtak og oppdatere forrigeBehandlingId hvis man må nullstille vedtaket`() {
            mockFinnSisteIverksatteBehandling(tidligereIverksattBehandling)

            behandlingPåVentService.taAvVent(behandlingId)

            verifyOrder {
                behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES)
                behandlingService.oppdaterForrigeBehandlingId(behandlingId, tidligereIverksattBehandling.id)
                nullstillVedtakService.nullstillVedtak(behandlingId)
            }
        }
    }

    private fun mockHentBehandling(status: BehandlingStatus, forrigeBehandlingId: UUID? = null) {
        every {
            behandlingService.hentBehandling(behandlingId)
        } returns behandling.copy(status = status, forrigeBehandlingId = forrigeBehandlingId)
    }

    private fun mockHentBehandlinger(vararg behandlinger: Behandling) {
        every { behandlingService.hentBehandlinger(fagsak.id) } returns behandlinger.toList()
    }

    private fun mockFinnSisteIverksatteBehandling(behandling: Behandling?) {
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
    }
}
