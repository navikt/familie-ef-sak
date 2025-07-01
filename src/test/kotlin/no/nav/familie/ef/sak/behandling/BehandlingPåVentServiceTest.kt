package no.nav.familie.ef.sak.behandling

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.SettPåVentRequest
import no.nav.familie.ef.sak.behandling.dto.TaAvVentStatus
import no.nav.familie.ef.sak.behandling.dto.beskrivelse
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveSubtype
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.vedtak.NullstillVedtakService
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.MappeDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.Month
import java.util.UUID

internal class BehandlingPåVentServiceTest {
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val taskService = mockk<TaskService>(relaxed = true)
    private val nullstillVedtakService = mockk<NullstillVedtakService>(relaxed = true)
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>()
    private val tilordnetRessursService: TilordnetRessursService = mockk<TilordnetRessursService>(relaxed = true)

    private val featureToggleService = mockk<FeatureToggleService>()

    private val behandlingPåVentService =
        BehandlingPåVentService(
            behandlingService,
            behandlingshistorikkService,
            taskService,
            nullstillVedtakService,
            oppgaveService,
            tilordnetRessursService,
        )
    val fagsak = fagsak()
    val tidligereIverksattBehandling = behandling(fagsak)
    val behandling = behandling(fagsak)
    val saksbehandling = saksbehandling(fagsak, behandling)
    val behandlingId = behandling.id

    @BeforeEach
    internal fun setUp() {
        mockkObject(SikkerhetContext)
        every { featureToggleService.isEnabled(any()) } returns true
        every { SikkerhetContext.hentSaksbehandler() } returns "bob"
        mockFinnSisteIverksatteBehandling(null)
        every { oppgaveService.finnMapper(any<String>()) } answers {
            val enhetsnr = firstArg<String>()
            listOf(
                MappeDto(
                    id = 101,
                    navn = "Mappe 1",
                    enhetsnr = enhetsnr,
                ),
                MappeDto(
                    id = 102,
                    navn = "Mappe 2",
                    enhetsnr = enhetsnr,
                ),
            )
        }
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true
        every { oppgaveService.hentOppgaveSomIkkeErFerdigstilt(any(), any()) } returns
            no.nav.familie.ef.sak.oppgave
                .Oppgave(behandlingId = UUID.randomUUID(), gsakOppgaveId = 123, type = Oppgavetype.BehandleSak)
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class SettPåVent {
        @Test
        fun `skal oppdatere oppgavebeskrivelse ved sett på vent - med saksbehandler, prioritet, frist og mappe`() {
            mockHentBehandling(BehandlingStatus.UTREDES)

            val oppgaveSlot = slot<Oppgave>()
            val oppgaveId: Long = 123
            val eksisterendeOppgave = oppgave(oppgaveId)

            every { oppgaveService.hentOppgave(oppgaveId) } returns eksisterendeOppgave

            justRun { oppgaveService.oppdaterOppgave(capture(oppgaveSlot)) }

            val settPåVentRequest = settPåVentRequest(oppgaveId, emptyList())

            behandlingPåVentService.settPåVent(
                behandlingId,
                settPåVentRequest,
            )

            verify { behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.SATT_PÅ_VENT) }
            verify {
                behandlingshistorikkService.opprettHistorikkInnslag(
                    behandlingId,
                    any(),
                    StegUtfall.SATT_PÅ_VENT,
                    null,
                )
            }
            verify {
                taskService.save(
                    coWithArg {
                        assertThat(it.type).isEqualTo(BehandlingsstatistikkTask.TYPE)
                        assertThat(it.payload).contains(Hendelse.VENTER.name)
                    },
                )
            }

            assertThat(oppgaveSlot.captured.beskrivelse).isNotEqualTo(eksisterendeOppgave.beskrivelse)
            assertThat(oppgaveSlot.captured.mappeId).isEqualTo(settPåVentRequest.mappe)
            assertThat(oppgaveSlot.captured.tilordnetRessurs).isEqualTo(settPåVentRequest.saksbehandler)
            assertThat(oppgaveSlot.captured.prioritet).isEqualTo(settPåVentRequest.prioritet)
            assertThat(oppgaveSlot.captured.fristFerdigstillelse).isEqualTo(settPåVentRequest.frist)
            assertThat(oppgaveSlot.captured.id).isEqualTo(settPåVentRequest.oppgaveId)
        }

        @Test
        fun `skal opprette vurder konsekvens oppgave - innstilling om utdanning`() {
            val oppgaveId = mockOppsettForAutomatiskeOppgaver()

            val oppfølgingsoppgaver = listOf(innstillingUtdanning)
            val settPåVentRequest =
                settPåVentRequest(
                    oppgaveId,
                    oppfølgingsoppgaver,
                )

            behandlingPåVentService.settPåVent(
                behandlingId,
                settPåVentRequest,
            )

            verify(exactly = 1) {
                taskService.save(
                    coWithArg {
                        assertThat(it.type).isEqualTo(OpprettOppgaveTask.TYPE)
                        assertThat(it.payload).contains(behandlingId.toString())
                        assertThat(it.payload).contains(Oppgavetype.VurderHenvendelse.name)
                        assertThat(it.payload).contains("Vi trenger en vurdering fra dere fordi bruker tar/skal ta utdanning.")
                    },
                )
            }
        }

        @Test
        fun `skal opprette vurder henvendelse oppgave - informere om søkt overgangsstønad`() {
            val oppgaveId = mockOppsettForAutomatiskeOppgaver()

            val oppfølgingsoppgaver = listOf(informereOmSøktStønad)
            val settPåVentRequest =
                settPåVentRequest(
                    oppgaveId,
                    oppfølgingsoppgaver,
                )

            behandlingPåVentService.settPåVent(
                behandlingId,
                settPåVentRequest,
            )

            verify(exactly = 1) {
                taskService.save(
                    coWithArg {
                        assertThat(it.type).isEqualTo(OpprettOppgaveTask.TYPE)
                        assertThat(it.payload).contains(behandlingId.toString())
                        assertThat(it.payload).contains(Oppgavetype.VurderHenvendelse.name)
                        assertThat(it.payload).contains("Bruker har søkt om overgangsstønad. Vurder oppfølging.")
                    },
                )
            }
        }

        @Test
        fun `skal opprette flere vurder henvendelse oppgaver mot lokalkontor i en request`() {
            val oppgaveId = mockOppsettForAutomatiskeOppgaver()

            val oppfølgingsoppgaver = listOf(informereOmSøktStønad, innstillingUtdanning)
            val settPåVentRequest =
                settPåVentRequest(
                    oppgaveId,
                    oppfølgingsoppgaver,
                )

            behandlingPåVentService.settPåVent(
                behandlingId,
                settPåVentRequest,
            )

            verify(exactly = 1) {
                taskService.save(
                    coWithArg {
                        assertThat(it.type).isEqualTo(OpprettOppgaveTask.TYPE)
                        assertThat(it.payload).contains(behandlingId.toString())
                        assertThat(it.payload).contains(Oppgavetype.VurderHenvendelse.name)
                        assertThat(it.payload).contains("Bruker har søkt om overgangsstønad. Vurder oppfølging.")
                    },
                )
            }

            verify(exactly = 1) {
                taskService.save(
                    coWithArg {
                        assertThat(it.type).isEqualTo(OpprettOppgaveTask.TYPE)
                        assertThat(it.payload).contains(behandlingId.toString())
                        assertThat(it.payload).contains(Oppgavetype.VurderHenvendelse.name)
                        assertThat(it.payload).contains(OppgaveBeskrivelse.innstillingOmBrukersUtdanning.subSequence(0, 50))
                    },
                )
            }
        }

        @Test
        fun `skal ikke kunne opprette informer om søkt overgangsstønad oppgave dersom behandlingen ikke er overgangsstønad`() {
            val oppgaveId = mockOppsettForAutomatiskeOppgaver(stønadType = StønadType.SKOLEPENGER)

            val oppfølgingsoppgaver = listOf(informereOmSøktStønad)
            val settPåVentRequest =
                settPåVentRequest(
                    oppgaveId,
                    oppfølgingsoppgaver,
                )

            val feil: Feil =
                assertThrows { behandlingPåVentService.settPåVent(behandlingId, settPåVentRequest) }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }

        @Test
        fun `skal ikke kunne opprette innstilling om utdanning oppgave dersom behandlingen hverken er overgangsstønad eller skolepenger`() {
            val oppgaveId = mockOppsettForAutomatiskeOppgaver(stønadType = StønadType.BARNETILSYN)

            val oppfølgingsoppgaver = listOf(innstillingUtdanning)
            val settPåVentRequest =
                settPåVentRequest(
                    oppgaveId,
                    oppfølgingsoppgaver,
                )

            val feil: Feil =
                assertThrows { behandlingPåVentService.settPåVent(behandlingId, settPåVentRequest) }

            assertThat(feil.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }

        @Test
        fun `skal ikke sette behandling på vent hvis den er sperret for redigering`() {
            mockHentBehandling(BehandlingStatus.FATTER_VEDTAK)

            val feil: ApiFeil =
                assertThrows { behandlingPåVentService.settPåVent(behandlingId, settPåVentRequest(1, emptyList())) }

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

        @Test
        internal fun `skal kunne ta av vent når en annen behandling er på vent`() {
            mockHentBehandling(BehandlingStatus.SATT_PÅ_VENT, forrigeBehandlingId = UUID.randomUUID())
            mockHentBehandlinger(behandling(fagsak, status = BehandlingStatus.SATT_PÅ_VENT))

            val kanTaAvVent = behandlingPåVentService.kanTaAvVent(behandlingId)

            assertThat(kanTaAvVent.status).isEqualTo(TaAvVentStatus.OK)
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
            val oppgaveId = 1234561L
            mockSettSaksbehandlerPåOppgave(oppgaveId)

            behandlingPåVentService.taAvVent(behandlingId)

            verify { behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES) }
            verify {
                behandlingshistorikkService.opprettHistorikkInnslag(
                    behandlingId,
                    any(),
                    StegUtfall.TATT_AV_VENT,
                    null,
                )
            }
            verify {
                taskService.save(
                    coWithArg {
                        assertThat(it.type).isEqualTo(BehandlingsstatistikkTask.TYPE)
                        assertThat(it.payload).contains(Hendelse.PÅBEGYNT.name)
                    },
                )
            }
            verify(exactly = 0) { nullstillVedtakService.nullstillVedtak(any()) }
            verify(exactly = 0) { behandlingService.oppdaterForrigeBehandlingId(any(), any()) }
            verify { oppgaveService.fordelOppgave(oppgaveId, "bob", any()) }
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
        internal fun `skal nullstille vedtak og oppdatere forrigeBehandlingId hvis man må nullstille vedtaket`() {
            val oppgaveId = 123456L
            mockSettSaksbehandlerPåOppgave(oppgaveId)
            mockFinnSisteIverksatteBehandling(tidligereIverksattBehandling)

            behandlingPåVentService.taAvVent(behandlingId)

            verifyOrder {
                behandlingService.oppdaterStatusPåBehandling(behandlingId, BehandlingStatus.UTREDES)
                behandlingService.oppdaterForrigeBehandlingId(behandlingId, tidligereIverksattBehandling.id)
                nullstillVedtakService.nullstillVedtak(behandlingId)
                oppgaveService.fordelOppgave(oppgaveId, "bob", any())
            }
        }

        private fun mockSettSaksbehandlerPåOppgave(oppgaveId: Long) {
            val oppgave = oppgave(oppgaveId)
            every { tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandling(behandlingId) } returns oppgave
            every { oppgaveService.fordelOppgave(any(), any(), any()) } returns oppgaveId
        }
    }

    @Nested
    inner class OppgaveSubtypeTest {
        @Test
        fun `skal returnere beskrivelse uten beskjed for info om søkt overgangsstønad`() {
            val subtype = OppgaveSubtype.INFORMERE_OM_SØKT_OVERGANGSSTØNAD
            val beskrivelse = OppgaveBeskrivelse.informereLokalkontorOmOvergangsstønad

            val beskrivelseOppgaveSubtype = subtype.beskrivelse("beskjed")

            assertThat(beskrivelseOppgaveSubtype).isEqualTo(beskrivelse)
        }

        @Test
        fun `skal returnere beskrivelse med ekstra beskjed for innstilling vedr utdanning`() {
            val subtype = OppgaveSubtype.INNSTILLING_VEDRØRENDE_UTDANNING
            val beskjed = "beskjed"
            val beskrivelse = "${OppgaveBeskrivelse.innstillingOmBrukersUtdanning}\n $beskjed \n"

            val beskrivelseOppgaveSubtype = subtype.beskrivelse(beskjed)

            assertThat(beskrivelseOppgaveSubtype).isEqualTo(beskrivelse)
        }

        @Test
        fun `skal returnere beskrivelse uten ekstra beskjed for innstilling vedr utdanning`() {
            val subtype = OppgaveSubtype.INNSTILLING_VEDRØRENDE_UTDANNING
            val beskjed = null
            val beskrivelse = OppgaveBeskrivelse.innstillingOmBrukersUtdanning
            val beskrivelseOppgaveSubtype = subtype.beskrivelse(beskjed)

            assertThat(beskrivelseOppgaveSubtype).isEqualTo(beskrivelse)
        }
    }

    private fun oppgave(oppgaveId: Long) =
        Oppgave(
            id = oppgaveId,
            tildeltEnhetsnr = "4489",
            tilordnetRessurs = "gammel saksbehandler",
            beskrivelse = "Gammel beskrivelse",
            mappeId = 101,
            fristFerdigstillelse = LocalDate.of(2002, Month.MARCH, 23).toString(),
            prioritet = OppgavePrioritet.NORM,
        )

    private fun settPåVentRequest(
        oppgaveId: Long,
        oppfølgingsoppgaver: List<OppgaveSubtype>,
    ) = SettPåVentRequest(
        oppgaveId = oppgaveId,
        saksbehandler = "ny saksbehandler",
        prioritet = OppgavePrioritet.HOY,
        frist = LocalDate.of(2002, Month.MARCH, 24).toString(),
        mappe = 102,
        beskrivelse = "Her er litt tekst fra saksbehandler",
        oppgaveVersjon = 1,
        oppfølgingsoppgaverMotLokalKontor = oppfølgingsoppgaver,
        innstillingsoppgaveBeskjed = "",
    )

    private fun mockHentBehandling(
        status: BehandlingStatus,
        forrigeBehandlingId: UUID? = null,
        stønadType: StønadType = StønadType.OVERGANGSSTØNAD,
    ) {
        every {
            behandlingService.hentBehandling(behandlingId)
        } returns behandling.copy(status = status, forrigeBehandlingId = forrigeBehandlingId)
        every { behandlingService.hentSaksbehandling(behandlingId) } returns saksbehandling.copy(stønadstype = stønadType)
    }

    private fun mockHentBehandlinger(vararg behandlinger: Behandling) {
        every { behandlingService.hentBehandlinger(fagsak.id) } returns behandlinger.toList()
    }

    private fun mockFinnSisteIverksatteBehandling(behandling: Behandling?) {
        every { behandlingService.finnSisteIverksatteBehandling(fagsak.id) } returns behandling
    }

    private fun mockOppsettForAutomatiskeOppgaver(stønadType: StønadType = StønadType.OVERGANGSSTØNAD): Long {
        mockHentBehandling(BehandlingStatus.UTREDES, null, stønadType)

        val oppgaveId: Long = 123
        val eksisterendeOppgave = oppgave(oppgaveId)

        every { oppgaveService.hentOppgave(oppgaveId) } returns eksisterendeOppgave
        justRun { oppgaveService.oppdaterOppgave(any()) }

        return oppgaveId
    }

    private val innstillingUtdanning = OppgaveSubtype.INNSTILLING_VEDRØRENDE_UTDANNING
    private val informereOmSøktStønad = OppgaveSubtype.INFORMERE_OM_SØKT_OVERGANGSSTØNAD
}
