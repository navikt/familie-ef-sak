package no.nav.familie.ef.sak.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID
import no.nav.familie.ef.sak.oppgave.Oppgave as EFOppgave

internal class HentIkkeFerdigstiltOppgaveServiceTest {

    private val oppgaveClient: OppgaveClient = mockk()
    private val oppgaveRepository: OppgaveRepository = mockk()

    private val hentIkkeFerdigstiltOppgaveService: HentIkkeFerdigstiltOppgaveService =
        HentIkkeFerdigstiltOppgaveService(oppgaveClient, oppgaveRepository)

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
                hentIkkeFerdigstiltOppgaveService.hentIkkeFerdigstiltOppgaveForBehandling(UUID.randomUUID())

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
                hentIkkeFerdigstiltOppgaveService.hentIkkeFerdigstiltOppgaveForBehandling(UUID.randomUUID())

            assertThat(hentetOppgave).isNull()
            verify(exactly = 0) { oppgaveClient.finnOppgaveMedId(any()) }
        }
    }

    companion object {
        private val oppgaveTyper = setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak)
        private val efOppgave =
            EFOppgave(behandlingId = UUID.randomUUID(), gsakOppgaveId = 1L, type = Oppgavetype.BehandleSak)
        private val oppgave = Oppgave(id = 1L)
    }
}
