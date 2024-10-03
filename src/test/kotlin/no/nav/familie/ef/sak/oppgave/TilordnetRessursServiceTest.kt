package no.nav.familie.ef.sak.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.dto.SaksbehandlerRolle
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.BEHANDLING_FERDIGSTILT
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.BEREGNE_YTELSE
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.BESLUTTE_VEDTAK
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.FERDIGSTILLE_BEHANDLING
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.LAG_SAKSBEHANDLINGSBLANKETT
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.PUBLISER_VEDTAKSHENDELSE
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.REVURDERING_ÅRSAK
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.SEND_TIL_BESLUTTER
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType.VILKÅR
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.oppgave.Oppgave as EFOppgave

internal class TilordnetRessursServiceTest {
    private val oppgaveClient: OppgaveClient = mockk()
    private val oppgaveRepository: OppgaveRepository = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()

    private val tilordnetRessursService: TilordnetRessursService =
        TilordnetRessursService(oppgaveClient, oppgaveRepository, featureToggleService, behandlingRepository)

    @BeforeEach
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns "NAV1234"
        every { featureToggleService.isEnabled(any()) } returns false
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class HentIkkeFerdigstiltOppgave {
        @Test
        internal fun `skal kunne hente ikke ferdigstilt oppgave på behandlingId`() {
            every {
                oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                    any(),
                    oppgaveTyperMedGodkjenneVedtak,
                )
            } answers { efOppgave(firstArg<UUID>()) }
            every { oppgaveClient.finnOppgaveMedId(any()) } answers { oppgave(firstArg<Long>()) }

            val behandlingId = UUID.randomUUID()
            val hentetOppgave =
                tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandling(behandlingId)

            assertThat(hentetOppgave?.id).isEqualTo(1L)
        }

        @Test
        internal fun `skal returnere null dersom efOppgave er null`() {
            every {
                oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                    any(),
                    oppgaveTyperMedGodkjenneVedtak,
                )
            } returns null
            every { oppgaveClient.finnOppgaveMedId(any()) } answers { oppgave(firstArg<Long>()) }

            val behandlingId = UUID.randomUUID()
            val hentetOppgave =
                tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandling(behandlingId)

            assertThat(hentetOppgave).isNull()
            verify(exactly = 0) { oppgaveClient.finnOppgaveMedId(any()) }
        }
    }

    @Nested
    inner class UtledSaksbehandlerRolle {
        @Test
        internal fun `skal returnere false dersom tilordnet ressurs er null`() {
            every {
                oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                    any(),
                    oppgaveTyper,
                )
            } answers { efOppgave(firstArg<UUID>()) }
            every { oppgaveClient.finnOppgaveMedId(any()) } answers { oppgave(firstArg<Long>()).copy(tilordnetRessurs = null) }

            val erSaksbehandlerEllerNull =
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(UUID.randomUUID())

            assertThat(erSaksbehandlerEllerNull).isFalse()
        }

        @Test
        internal fun `skal returnere true dersom returnert oppgave er null`() {
            every {
                oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                    any(),
                    oppgaveTyper,
                )
            } answers { null }

            val erSaksbehandlerEllerNull =
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(UUID.randomUUID())

            assertThat(erSaksbehandlerEllerNull).isTrue()
        }

        @Test
        internal fun `skal returnere true dersom tilordnet ressurs er innlogget saksbehander`() {
            every {
                oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                    any(),
                    oppgaveTyper,
                )
            } answers { efOppgave(firstArg<UUID>()) }
            every { oppgaveClient.finnOppgaveMedId(any()) } answers { oppgave(firstArg<Long>()).copy(tilordnetRessurs = "NAV1234", tema = Tema.ENF) }

            val erSaksbehandlerEllerNull =
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(UUID.randomUUID())

            assertThat(erSaksbehandlerEllerNull).isTrue()
        }

        @Test
        internal fun `skal returnere false dersom tilordnet ressurs ikke er innlogget saksbehander`() {
            every {
                oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                    any(),
                    oppgaveTyper,
                )
            } answers { efOppgave(firstArg<UUID>()) }
            every { oppgaveClient.finnOppgaveMedId(any()) } answers { oppgave(firstArg<Long>()).copy(tilordnetRessurs = "NAV2345") }

            val erSaksbehandlerEllerNull =
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(UUID.randomUUID())

            assertThat(erSaksbehandlerEllerNull).isFalse()
        }

        @Test
        internal fun `skal returnere false dersom innlogget saksbehandler er utvikler med veilederrolle`() {
            every {
                oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                    any(),
                    oppgaveTyper,
                )
            } answers { efOppgave(firstArg<UUID>()) }
            every { oppgaveClient.finnOppgaveMedId(any()) } answers { oppgave(firstArg<Long>()).copy(tilordnetRessurs = "NAV2345") }
            every { featureToggleService.isEnabled(any()) } returns true

            val erSaksbehandlerEllerNull =
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(UUID.randomUUID())

            assertThat(erSaksbehandlerEllerNull).isFalse()
        }
    }

    @Nested
    inner class UtledAnsvarligSaksbehandler {
        @Test
        internal fun `skal utlede at saksbehandlers rolle er INNLOGGET SAKSBEHANDLER`() {
            val saksbehandler = saksbehandler(UUID.randomUUID(), "Skywalker", "Anakin", "NAV1234")
            val oppgave = Oppgave(tilordnetRessurs = "NAV1234", tema = Tema.ENF)

            every { oppgaveClient.hentSaksbehandlerInfo("NAV1234") } returns saksbehandler

            val saksbehandlerDto = tilordnetRessursService.utledAnsvarligSaksbehandlerForOppgave(oppgave)

            assertThat(saksbehandlerDto.fornavn).isEqualTo("Anakin")
            assertThat(saksbehandlerDto.etternavn).isEqualTo("Skywalker")
            assertThat(saksbehandlerDto.rolle).isEqualTo(SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER)
        }

        @Test
        internal fun `skal utlede at saksbehandlers rolle er ANNEN SAKSBEHANDLER`() {
            val saksbehandler = saksbehandler(UUID.randomUUID(), "Vader", "Darth", "NAV2345")
            val oppgave = Oppgave(tilordnetRessurs = "NAV2345", tema = Tema.ENF)

            every { oppgaveClient.hentSaksbehandlerInfo("NAV2345") } returns saksbehandler

            val saksbehandlerDto = tilordnetRessursService.utledAnsvarligSaksbehandlerForOppgave(oppgave)

            assertThat(saksbehandlerDto.fornavn).isEqualTo("Darth")
            assertThat(saksbehandlerDto.etternavn).isEqualTo("Vader")
            assertThat(saksbehandlerDto.rolle).isEqualTo(SaksbehandlerRolle.ANNEN_SAKSBEHANDLER)
        }

        @Test
        internal fun `skal utlede at saksbehandlers rolle er IKKE SATT`() {
            val oppgave = Oppgave(tilordnetRessurs = null, tema = Tema.ENF)

            val saksbehandlerDto = tilordnetRessursService.utledAnsvarligSaksbehandlerForOppgave(oppgave)

            assertThat(saksbehandlerDto.fornavn).isEqualTo("")
            assertThat(saksbehandlerDto.etternavn).isEqualTo("")
            assertThat(saksbehandlerDto.rolle).isEqualTo(SaksbehandlerRolle.IKKE_SATT)
            verify(exactly = 0) { oppgaveClient.hentSaksbehandlerInfo(any()) }
        }

        @Test
        internal fun `skal utlede at saksbehandlers rolle er OPPGAVE FINNES IKKE`() {
            val saksbehandlerDto = tilordnetRessursService.utledAnsvarligSaksbehandlerForOppgave(null)

            assertThat(saksbehandlerDto.fornavn).isEqualTo("")
            assertThat(saksbehandlerDto.etternavn).isEqualTo("")
            assertThat(saksbehandlerDto.rolle).isEqualTo(SaksbehandlerRolle.OPPGAVE_FINNES_IKKE)
            verify(exactly = 0) { oppgaveClient.hentSaksbehandlerInfo(any()) }
        }

        @Test
        internal fun `skal utlede at saksbehandlers rolle er UTVIKLER MED VEILEDERROLLE`() {
            val saksbehandler = saksbehandler(UUID.randomUUID(), "Vader", "Darth", "NAV1234")
            val oppgave = Oppgave(tilordnetRessurs = "NAV1234")

            every { oppgaveClient.hentSaksbehandlerInfo("NAV1234") } returns saksbehandler
            every { featureToggleService.isEnabled(any()) } returns true

            val saksbehandlerDto = tilordnetRessursService.utledAnsvarligSaksbehandlerForOppgave(oppgave)

            assertThat(saksbehandlerDto.fornavn).isEqualTo("Darth")
            assertThat(saksbehandlerDto.etternavn).isEqualTo("Vader")
            assertThat(saksbehandlerDto.rolle).isEqualTo(SaksbehandlerRolle.UTVIKLER_MED_VEILDERROLLE)
        }

        @Test
        internal fun `skal utlede at saksbehandlers rolle er OPPGAVE_TILHØRER_IKKE_ENF`() {
            val saksbehandler = saksbehandler(UUID.randomUUID(), "Vader", "Darth", "NAV2345")
            val oppgave = Oppgave(tilordnetRessurs = "NAV2345", tema = Tema.BAR)

            every { oppgaveClient.hentSaksbehandlerInfo("NAV2345") } returns saksbehandler

            val saksbehandlerDto = tilordnetRessursService.utledAnsvarligSaksbehandlerForOppgave(oppgave)

            assertThat(saksbehandlerDto.fornavn).isEqualTo("Darth")
            assertThat(saksbehandlerDto.etternavn).isEqualTo("Vader")
            assertThat(saksbehandlerDto.rolle).isEqualTo(SaksbehandlerRolle.OPPGAVE_TILHØRER_IKKE_ENF)
        }
    }

    @Test
    fun `Skal kunne hente ansvarlig saksbehandler gitt en nav ident`() {
        every { oppgaveClient.hentSaksbehandlerInfo(any()) } returns saksbehandler
        val ansvarligSaksbehandler = tilordnetRessursService.hentSaksbehandlerInfo("Z999999")

        assertThat(ansvarligSaksbehandler).isEqualTo(saksbehandler)
    }

    @Test
    fun `skal utføre kall mot åpne oppgaver med riktig oppgavetype basert på stegtypen i behandlingen`() {
        every { oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(any(), any()) } returns null
        val behandlesakOppgavetyper = listOf(REVURDERING_ÅRSAK, VILKÅR, BEREGNE_YTELSE, SEND_TIL_BESLUTTER)
        val godkjenneVedtakOppgavetyper = listOf(BESLUTTE_VEDTAK)
        val ingenOppgavetyper = listOf(StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT, LAG_SAKSBEHANDLINGSBLANKETT,FERDIGSTILLE_BEHANDLING, PUBLISER_VEDTAKSHENDELSE, BEHANDLING_FERDIGSTILT)

        behandlesakOppgavetyper.forEach { steg ->
            val behandlingId = UUID.randomUUID()
            every { behandlingRepository.findByIdOrThrow(any()) } returns behandling(fagsak(), steg = steg)
            tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandlingGittStegtype(behandlingId)
            verify { oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(behandlingId, setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak)) }
        }

        godkjenneVedtakOppgavetyper.forEach { steg ->
            val behandlingId = UUID.randomUUID()
            every { behandlingRepository.findByIdOrThrow(any()) } returns behandling(fagsak(), steg = steg)
            tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandlingGittStegtype(behandlingId)
            verify { oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(behandlingId, setOf(Oppgavetype.GodkjenneVedtak)) }
        }

        ingenOppgavetyper.forEach { steg ->
            val behandlingId = UUID.randomUUID()
            every { behandlingRepository.findByIdOrThrow(any()) } returns behandling(fagsak(), steg = steg)
            tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandlingGittStegtype(behandlingId)
            verify { oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(behandlingId, setOf()) }
        }

    }

    private fun efOppgave(behandlingId: UUID) =
        EFOppgave(behandlingId = behandlingId, gsakOppgaveId = 1L, type = Oppgavetype.BehandleSak)

    private fun oppgave(oppgaveId: Long) = Oppgave(id = oppgaveId)

    private fun saksbehandler(
        azureId: UUID,
        etternavn: String,
        fornavn: String,
        navIdent: String,
    ) = Saksbehandler(
        azureId = azureId,
        enhet = "4405",
        etternavn = etternavn,
        fornavn = fornavn,
        navIdent = navIdent,
    )

    companion object {
        private val oppgaveTyper = setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak)
        private val oppgaveTyperMedGodkjenneVedtak = oppgaveTyper + Oppgavetype.GodkjenneVedtak

        private val saksbehandler = Saksbehandler(UUID.randomUUID(), "Z999999", "Darth", "Vader", "4405")
    }
}
