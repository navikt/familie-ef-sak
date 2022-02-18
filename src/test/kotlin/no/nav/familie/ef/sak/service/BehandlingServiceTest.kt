package no.nav.familie.ef.sak.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.BEHANDLES_I_GOSYS
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.FEILREGISTRERT
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.TRUKKET_TILBAKE
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.felles.util.mockFeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingServiceTest {

    private val behandlingRepository: BehandlingRepository = mockk()
    private val behandlingshistorikkService: BehandlingshistorikkService = mockk(relaxed = true)
    private val taskService: TaskService = mockk(relaxed = true)
    private val behandlingService =
            BehandlingService(mockk(),
                              behandlingRepository,
                              behandlingshistorikkService,
                              taskService,
                              mockk(),
                              mockFeatureToggleService())
    private val behandlingSlot = slot<Behandling>()

    @BeforeAll
    fun setUp() {
        mockkObject(SikkerhetContext)
        every {
            behandlingRepository.update(capture(behandlingSlot))
        } answers {
            behandlingSlot.captured
        }
        every { SikkerhetContext.hentSaksbehandler(true) } returns "bob"
    }

    @AfterAll
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @BeforeEach
    fun reset() {
        clearAllMocks(answers = false)
    }

    @Nested
    inner class HenleggBehandling {

        @Test
        internal fun `skal henlegge behandling som er blankett og status utredes`() {
            val behandling = behandling(fagsak(), type = BehandlingType.BLANKETT, status = BehandlingStatus.UTREDES)
            henleggOgForventOk(behandling, BEHANDLES_I_GOSYS)
        }

        @Test
        internal fun `skal henlegge behandling som er blankett og status opprettet`() {
            val behandling = behandling(fagsak(), type = BehandlingType.BLANKETT, status = BehandlingStatus.OPPRETTET)
            henleggOgForventOk(behandling, BEHANDLES_I_GOSYS)
        }

        @Test
        internal fun `skal kunne henlegge behandling som er førstegangsbehandling`() {
            val behandling = behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.UTREDES)
            henleggOgForventOk(behandling, henlagtÅrsak = FEILREGISTRERT)
        }

        @Test
        internal fun `skal kunne henlegge behandling som er revurdering`() {
            val behandling = behandling(fagsak(), type = BehandlingType.REVURDERING, status = BehandlingStatus.UTREDES)
            henleggOgForventOk(behandling, FEILREGISTRERT)
        }

        private fun henleggOgForventOk(behandling: Behandling, henlagtÅrsak: HenlagtÅrsak) {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling

            behandlingService.henleggBehandling(behandling.id, HenlagtDto(henlagtÅrsak))
            assertThat(behandlingSlot.captured.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
            assertThat(behandlingSlot.captured.resultat).isEqualTo(BehandlingResultat.HENLAGT)
            assertThat(behandlingSlot.captured.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling hvor vedtak fattes`() {
            val behandling =
                    behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.FATTER_VEDTAK)
            henleggOgForventFeilmelding(behandling, FEILREGISTRERT)
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling som er iverksatt`() {
            val behandling = behandling(fagsak(),
                                        type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                        status = BehandlingStatus.IVERKSETTER_VEDTAK)
            henleggOgForventFeilmelding(behandling, TRUKKET_TILBAKE)
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling som er ferdigstilt`() {
            val behandling =
                    behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.FERDIGSTILT)
            henleggOgForventFeilmelding(behandling, TRUKKET_TILBAKE)
        }

        private fun henleggOgForventFeilmelding(behandling: Behandling, henlagtÅrsak: HenlagtÅrsak) {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling

            val feil: Feil = assertThrows {
                behandlingService.henleggBehandling(behandling.id, HenlagtDto(henlagtÅrsak))
            }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class SettPåVent {

        private val behandling = behandling()

        @Test
        fun `skal sette behandling på vent hvis den kan redigeres og sende melding til DVH`() {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling.copy(status = BehandlingStatus.UTREDES)

            behandlingService.settPåVent(UUID.randomUUID())

            assertThat(behandlingSlot.captured.status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)
            verify {
                taskService.save(coWithArg {
                    assertThat(it.type).isEqualTo(BehandlingsstatistikkTask.TYPE)
                    assertThat(it.payload).contains(Hendelse.VENTER.name)
                })
            }
        }

        @Test
        fun `skal ikke sette behandling på vent hvis den er sperret for redigering`() {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling.copy(status = BehandlingStatus.FATTER_VEDTAK)

            val feil: ApiFeil = assertThrows { behandlingService.settPåVent(UUID.randomUUID()) }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class TaAvVent {

        private val behandling = behandling()

        @Test
        fun `skal ta behandling av vent og sende melding til DVH`() {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling.copy(status = BehandlingStatus.SATT_PÅ_VENT)

            behandlingService.taAvVent(UUID.randomUUID())

            assertThat(behandlingSlot.captured.status).isEqualTo(BehandlingStatus.UTREDES)
            verify {
                taskService.save(coWithArg {
                    assertThat(it.type).isEqualTo(BehandlingsstatistikkTask.TYPE)
                    assertThat(it.payload).contains(Hendelse.PÅBEGYNT.name)
                })
            }
        }

        @Test
        fun `skal feile hvis behandling ikke er på vent`() {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling.copy(status = BehandlingStatus.FATTER_VEDTAK)

            val feil: ApiFeil = assertThrows { behandlingService.taAvVent(UUID.randomUUID()) }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
