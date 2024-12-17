package no.nav.familie.ef.sak.service

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.FEILREGISTRERT
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.TRUKKET_TILBAKE
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.util.mockFeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BehandlingServiceTest {
    private val behandlingRepository: BehandlingRepository = mockk()
    private val behandlingshistorikkService: BehandlingshistorikkService = mockk(relaxed = true)
    private val taskService: TaskService = mockk(relaxed = true)
    private val tilordnetRessursService: TilordnetRessursService = mockk(relaxed = true)
    private val behandlingService =
        BehandlingService(
            mockk(),
            behandlingRepository,
            behandlingshistorikkService,
            taskService,
            mockFeatureToggleService(),
            tilordnetRessursService,
        )
    private val behandlingSlot = slot<Behandling>()

    @BeforeAll
    fun setUp() {
        mockkObject(SikkerhetContext)
        every {
            behandlingRepository.update(capture(behandlingSlot))
        } answers {
            behandlingSlot.captured
        }
        every { SikkerhetContext.hentSaksbehandler() } returns "bob"
    }

    @AfterAll
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @BeforeEach
    fun reset() {
        clearAllMocks(answers = false)
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true
    }

    @Test
    internal fun `skal feile hvis krav mottatt er frem i tid`() {
        assertThrows<ApiFeil> {
            behandlingService.opprettBehandling(
                status = BehandlingStatus.OPPRETTET,
                stegType = StegType.VILKÅR,
                behandlingsårsak = BehandlingÅrsak.PAPIRSØKNAD,
                kravMottatt = LocalDate.now().plusDays(1),
                erMigrering = false,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsakId = UUID.randomUUID(),
            )
        }
    }

    @Nested
    inner class HenleggBehandling {
        @Test
        internal fun `skal kunne henlegge behandling som er førstegangsbehandling`() {
            val behandling =
                behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.UTREDES)
            henleggOgForventOk(behandling, henlagtÅrsak = FEILREGISTRERT)
        }

        @Test
        internal fun `skal kunne henlegge behandling dersom tilhørende oppgave har annet tema enn ENF`() {
            val behandling =
                behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.UTREDES)
            every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true
            henleggOgForventOk(behandling, henlagtÅrsak = FEILREGISTRERT, false)
        }

        @Test
        internal fun `skal kunne henlegge behandling som er revurdering`() {
            val behandling = behandling(fagsak(), type = BehandlingType.REVURDERING, status = BehandlingStatus.UTREDES)
            henleggOgForventOk(behandling, FEILREGISTRERT)
        }

        private fun henleggOgForventOk(
            behandling: Behandling,
            henlagtÅrsak: HenlagtÅrsak,
            henleggTilhørendeOppgave: Boolean = true,
        ) {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling

            behandlingService.henleggBehandling(behandling.id, HenlagtDto(henlagtÅrsak, saksbehandlerSignatur = ""), henleggTilhørendeOppgave)
            assertThat(behandlingSlot.captured.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
            assertThat(behandlingSlot.captured.resultat).isEqualTo(BehandlingResultat.HENLAGT)
            assertThat(behandlingSlot.captured.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
            assertThat(behandlingSlot.captured.vedtakstidspunkt).isNotNull
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling hvor vedtak fattes`() {
            val behandling =
                behandling(
                    fagsak(),
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.FATTER_VEDTAK,
                )
            henleggOgForventApiFeilmelding(behandling, FEILREGISTRERT)
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling som er iverksatt`() {
            val behandling =
                behandling(
                    fagsak(),
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.IVERKSETTER_VEDTAK,
                )
            henleggOgForventApiFeilmelding(behandling, TRUKKET_TILBAKE)
        }

        @Test
        internal fun `skal ikke kunne henlegge behandling som er ferdigstilt`() {
            val behandling =
                behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.FERDIGSTILT)
            henleggOgForventApiFeilmelding(behandling, TRUKKET_TILBAKE)
        }

        private fun henleggOgForventApiFeilmelding(
            behandling: Behandling,
            henlagtÅrsak: HenlagtÅrsak,
        ) {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling

            val feil: ApiFeil =
                assertThrows {
                    behandlingService.henleggBehandling(behandling.id, HenlagtDto(henlagtÅrsak, saksbehandlerSignatur = ""))
                }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(feil.feil).contains("Kan ikke henlegge en behandling med status")
        }
    }

    @Nested
    inner class OppdaterResultatPåBehandling {
        private val behandling = behandling()

        @Test
        internal fun `skal sette vedtakstidspunkt når man setter resultat`() {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling.copy(resultat = BehandlingResultat.IKKE_SATT)
            behandlingService.oppdaterResultatPåBehandling(UUID.randomUUID(), BehandlingResultat.INNVILGET)
        }

        @Test
        internal fun `skal feile hvis resultatet på behandlingen allerede er satt`() {
            every {
                behandlingRepository.findByIdOrThrow(any())
            } returns behandling.copy(resultat = BehandlingResultat.INNVILGET)
            assertThatThrownBy {
                behandlingService.oppdaterResultatPåBehandling(UUID.randomUUID(), BehandlingResultat.INNVILGET)
            }.hasMessageContaining("Kan ikke endre resultat på behandling når resultat")
        }

        @Test
        internal fun `skal feile hvis man setter IKKE_SATT`() {
            assertThatThrownBy {
                behandlingService.oppdaterResultatPåBehandling(UUID.randomUUID(), BehandlingResultat.IKKE_SATT)
            }.hasMessageContaining("Må sette et endelig resultat")
        }
    }

    @Test
    internal fun `skal ikke kunne henlegge behandling hvor innlogget saksbehandler er eier av behandling`() {
        val behandling =
            behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, status = BehandlingStatus.UTREDES)

        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns false

        every {
            behandlingRepository.findByIdOrThrow(any())
        } returns behandling

        val feil: ApiFeil =
            assertThrows {
                behandlingService.henleggBehandling(behandling.id, HenlagtDto(FEILREGISTRERT, saksbehandlerSignatur = ""))
            }

        assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(feil.feil).isEqualTo("Behandlingen har en annen eier og kan derfor ikke henlegges av deg")
    }

    @Nested
    inner class HentBehandlinger {
        @Test
        internal fun `skal sortere behandlinger etter vedtakstidspunkt og til sist uten vedtakstidspunkt`() {
            val tiDagerSiden = LocalDateTime.now().minusDays(10)
            val femFagerSiden = LocalDateTime.now().minusDays(5)
            val now = LocalDateTime.now()
            val behandling1 = opprettBehandling(femFagerSiden, tiDagerSiden, tiDagerSiden)
            val behandling2 = opprettBehandling(null, femFagerSiden, femFagerSiden)
            val behandling3 = opprettBehandling(tiDagerSiden, now, now)
            every {
                behandlingRepository.findByFagsakId(any())
            } returns listOf(behandling1, behandling2, behandling3)

            val hentBehandlinger = behandlingService.hentBehandlinger(UUID.randomUUID())
            assertThat(hentBehandlinger)
                .containsExactly(behandling3, behandling1, behandling2)
        }

        private fun opprettBehandling(
            vedtakstidspunkt: LocalDateTime?,
            opprettetTid: LocalDateTime,
            endretTid: LocalDateTime,
        ) = behandling(vedtakstidspunkt = vedtakstidspunkt).copy(
            sporbar =
                Sporbar(
                    opprettetTid = opprettetTid,
                    endret = Endret(endretTid = endretTid),
                ),
        )
    }
}
