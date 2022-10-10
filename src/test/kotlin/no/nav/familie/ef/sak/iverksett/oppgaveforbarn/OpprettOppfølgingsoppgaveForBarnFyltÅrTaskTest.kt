package no.nav.familie.ef.sak.no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.Alder
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.OpprettOppfølgingsoppgaveForBarnFyltÅrTask
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.OpprettOppgavePayload
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class OpprettOppfølgingsoppgaveForBarnFyltÅrTaskTest {

    val oppgaveRepository = mockk<OppgaveRepository>()
    val oppgaveService = mockk<OppgaveService>()

    val opprettOppfølgingsoppgaveForBarnFyltÅrTask = OpprettOppfølgingsoppgaveForBarnFyltÅrTask(
        oppgaveRepository,
        oppgaveService
    )
    val oppgaveClient = mockk<OppgaveClient>()
    val opprettOppgaveRequestSlot = slot<OpprettOppgaveRequest>()

    @BeforeEach
    internal fun setUp() {
        every { oppgaveClient.opprettOppgave(capture(opprettOppgaveRequestSlot)) } returns 1
    }

    @Test
    fun `oppgave skal puttes i hendelse-mappe`() {
        val oppfølgingsoppgaveTask = OpprettOppfølgingsoppgaveForBarnFyltÅrTask.opprettTask(
            OpprettOppgavePayload(
                UUID.randomUUID(),
                "1",
                "2",
                Alder.ETT_ÅR
            )
        )

        opprettOppfølgingsoppgaveForBarnFyltÅrTask.doTask(oppfølgingsoppgaveTask)

        assertThat(opprettOppgaveRequestSlot.captured.mappeId).isEqualTo(1)
    }
}
