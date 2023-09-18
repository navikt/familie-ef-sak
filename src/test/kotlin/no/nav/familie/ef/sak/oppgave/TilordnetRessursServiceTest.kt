package no.nav.familie.ef.sak.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID
import no.nav.familie.ef.sak.oppgave.Oppgave as EFOppgave

internal class TilordnetRessursServiceTest {

    private val oppgaveClient: OppgaveClient = mockk()
    private val oppgaveRepository: OppgaveRepository = mockk()

    private val tilordnetRessursService: TilordnetRessursService =
        TilordnetRessursService(oppgaveClient, oppgaveRepository)

    @BeforeEach
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns "NAV1234"
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
                    oppgaveTyper,
                )
            } returns efOppgave
            every { oppgaveClient.finnOppgaveMedId(any()) } returns oppgave

            val hentetOppgave =
                tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandling(UUID.randomUUID())

            assertThat(hentetOppgave).isEqualTo(oppgave)
        }

        @Test
        internal fun `skal returnere null dersom efOppgave er null`() {
            every {
                oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                    any(),
                    oppgaveTyper,
                )
            } returns null
            every { oppgaveClient.finnOppgaveMedId(any()) } returns oppgave

            val hentetOppgave =
                tilordnetRessursService.hentIkkeFerdigstiltOppgaveForBehandling(UUID.randomUUID())

            assertThat(hentetOppgave).isNull()
            verify(exactly = 0) { oppgaveClient.finnOppgaveMedId(any()) }
        }
    }

    @Nested
    inner class utledEierskapTilOppgave {

        @Test
        internal fun `skal returnere true dersom tilordnet ressurs er null`() {
            every {
                oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                    any(),
                    oppgaveTyper,
                )
            } returns efOppgave
            every { oppgaveClient.finnOppgaveMedId(any()) } returns oppgave.copy(tilordnetRessurs = null)

            val erSaksbehandlerEllerNull = tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandlerEllerNull(UUID.randomUUID())

            assertThat(erSaksbehandlerEllerNull).isTrue()
        }

        @Test
        internal fun `skal returnere true dersom tilordnet ressurs er innlogget saksbehander`() {
            every {
                oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                    any(),
                    oppgaveTyper,
                )
            } returns efOppgave
            every { oppgaveClient.finnOppgaveMedId(any()) } returns oppgave.copy(tilordnetRessurs = "NAV1234")

            val erSaksbehandlerEllerNull = tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandlerEllerNull(UUID.randomUUID())

            assertThat(erSaksbehandlerEllerNull).isTrue()
        }

        @Test
        internal fun `skal returnere false dersom tilordnet ressurs ikke er innlogget saksbehander`() {
            every {
                oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                    any(),
                    oppgaveTyper,
                )
            } returns efOppgave
            every { oppgaveClient.finnOppgaveMedId(any()) } returns oppgave.copy(tilordnetRessurs = "NAV2345")

            val erSaksbehandlerEllerNull = tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandlerEllerNull(UUID.randomUUID())

            assertThat(erSaksbehandlerEllerNull).isFalse()
        }

    }

    companion object {
        private val oppgaveTyper = setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak)
        private val efOppgave =
            EFOppgave(behandlingId = UUID.randomUUID(), gsakOppgaveId = 1L, type = Oppgavetype.BehandleSak)
        private val oppgave = Oppgave(id = 1L)
    }
}
