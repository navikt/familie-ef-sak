package no.nav.familie.ef.sak.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.dto.SaksbehandlerRolle
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
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
                    oppgaveTyper,
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
        internal fun `skal returnere true dersom tilordnet ressurs er null`() {
            every {
                oppgaveRepository.findByBehandlingIdAndErFerdigstiltIsFalseAndTypeIn(
                    any(),
                    oppgaveTyper,
                )
            } answers { efOppgave(firstArg<UUID>()) }
            every { oppgaveClient.finnOppgaveMedId(any()) } answers { oppgave(firstArg<Long>()).copy(tilordnetRessurs = null) }

            val erSaksbehandlerEllerNull =
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandlerEllerNull(UUID.randomUUID())

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
            every { oppgaveClient.finnOppgaveMedId(any()) } answers { oppgave(firstArg<Long>()).copy(tilordnetRessurs = "NAV1234") }

            val erSaksbehandlerEllerNull =
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandlerEllerNull(UUID.randomUUID())

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
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandlerEllerNull(UUID.randomUUID())

            assertThat(erSaksbehandlerEllerNull).isFalse()
        }
    }

    @Nested
    inner class MapSaksbehandlerDto {

        @Test
        internal fun `skal mappe INNLOGGET SAKSBEHANDLER til SaksbehandlerDto`() {
            val azureId = UUID.randomUUID()
            val saksbehandler = saksbehandler(azureId, "4405", "Vader", "Darth", "NAV1234")

            val saksbehandlerDto = tilordnetRessursService.mapTilSaksbehandlerDto(saksbehandler)

            assertThat(saksbehandlerDto.azureId).isEqualTo(azureId)
            assertThat(saksbehandlerDto.enhet).isEqualTo(saksbehandler.enhet)
            assertThat(saksbehandlerDto.fornavn).isEqualTo(saksbehandler.fornavn)
            assertThat(saksbehandlerDto.etternavn).isEqualTo(saksbehandler.etternavn)
            assertThat(saksbehandlerDto.navIdent).isEqualTo(saksbehandler.navIdent)
            assertThat(saksbehandlerDto.rolle).isEqualTo(SaksbehandlerRolle.INNLOGGET_SAKSBEHANDLER)
        }

        @Test
        internal fun `skal mappe ANNEN SAKSBEHANDLER til SaksbehandlerDto`() {
            val azureId = UUID.randomUUID()
            val saksbehandler = saksbehandler(azureId, "4405", "Vader", "Darth", "NAV2345")

            val saksbehandlerDto = tilordnetRessursService.mapTilSaksbehandlerDto(saksbehandler)

            assertThat(saksbehandlerDto.azureId).isEqualTo(azureId)
            assertThat(saksbehandlerDto.enhet).isEqualTo(saksbehandler.enhet)
            assertThat(saksbehandlerDto.fornavn).isEqualTo(saksbehandler.fornavn)
            assertThat(saksbehandlerDto.etternavn).isEqualTo(saksbehandler.etternavn)
            assertThat(saksbehandlerDto.navIdent).isEqualTo(saksbehandler.navIdent)
            assertThat(saksbehandlerDto.rolle).isEqualTo(SaksbehandlerRolle.ANNEN_SAKSBEHANDLER)
        }

        @Test
        internal fun `skal mappe IKKE SATT til SaksbehandlerDto`() {
            val saksbehandlerDto = tilordnetRessursService.mapTilSaksbehandlerDto(null)

            assertThat(saksbehandlerDto.enhet).isEqualTo("")
            assertThat(saksbehandlerDto.fornavn).isEqualTo("")
            assertThat(saksbehandlerDto.etternavn).isEqualTo("")
            assertThat(saksbehandlerDto.navIdent).isEqualTo("")
            assertThat(saksbehandlerDto.rolle).isEqualTo(SaksbehandlerRolle.IKKE_SATT)
        }
    }

    private fun efOppgave(behandlingId: UUID) =
        EFOppgave(behandlingId = behandlingId, gsakOppgaveId = 1L, type = Oppgavetype.BehandleSak)

    private fun oppgave(oppgaveId: Long) = Oppgave(id = oppgaveId)

    private fun saksbehandler(
        azureId: UUID,
        enhet: String,
        etternavn: String,
        fornavn: String,
        navIdent: String,
    ) = Saksbehandler(
        azureId = azureId,
        enhet = enhet,
        etternavn = etternavn,
        fornavn = fornavn,
        navIdent = navIdent,
    )

    companion object {

        private val oppgaveTyper = setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak)
    }
}
