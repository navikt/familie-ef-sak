package no.nav.familie.ef.sak.behandlingsflyt.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Properties
import java.util.UUID

internal class OpprettOppgaveTaskTest {
    val oppgaveService = mockk<OppgaveService>()
    val behandlingService = mockk<BehandlingService>()

    val opprettOppgaveTask = OpprettOppgaveTask(oppgaveService, behandlingService)

    val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        every { oppgaveService.opprettOppgave(any(), any(), any()) } returns 1L
        every { oppgaveService.opprettOppgaveUtenÅLagreIRepository(any(), any(), any(), any(), any()) } returns 2L
    }

    @Test
    fun `skal ikke opprette behandle sak oppgave om status er låst for videre redigering`() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(id = behandlingId, status = BehandlingStatus.FATTER_VEDTAK)

        val task =
            OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTask.OpprettOppgaveTaskData(
                    behandlingId = behandlingId,
                    oppgavetype = Oppgavetype.BehandleSak,
                ),
            )

        opprettOppgaveTask.doTask(task)
        verifyOpprettOppgaveMedLagringKall(0)
        verifyOpprettOppgaveUtenLagringKall()
    }

    @Test
    fun `skal opprette behandle sak oppgave om behandling ikke er låst for videre redigering`() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(id = behandlingId, status = BehandlingStatus.UTREDES)

        val task =
            OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTask.OpprettOppgaveTaskData(
                    behandlingId = behandlingId,
                    oppgavetype = Oppgavetype.BehandleSak,
                ),
            )

        opprettOppgaveTask.doTask(task)
        verifyOpprettOppgaveMedLagringKall(1)
        verifyOpprettOppgaveUtenLagringKall()
    }

    @Test
    fun `skal lagre ned oppgave om oppgavetypen er vurder henvendelse`() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(id = behandlingId, status = BehandlingStatus.UTREDES)

        val task =
            OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTask.OpprettOppgaveTaskData(
                    behandlingId = behandlingId,
                    oppgavetype = Oppgavetype.VurderHenvendelse,
                ),
            )

        opprettOppgaveTask.doTask(task)
        verifyOpprettOppgaveMedLagringKall(1)
    }

    @Test
    fun `skal sende nokkelord og høy prioritet videre når oppgaven opprettes`() {
        val nokkelord = setOf("EØS", "Selvstendig næringsdrivende")
        val beskrivelse = "EØS, Selvstendig næringsdrivende. Sendt til godkjenning."
        every { oppgaveService.opprettOppgave(any(), any(), nokkelord = nokkelord, prioritet = OppgavePrioritet.HOY, beskrivelse = beskrivelse) } returns 1L
        val task =
            OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTask.OpprettOppgaveTaskData(
                    behandlingId = behandlingId,
                    oppgavetype = Oppgavetype.GodkjenneVedtak,
                    beskrivelse = beskrivelse,
                    erHøyPrioritet = true,
                    nokkelord = nokkelord,
                ),
            )

        opprettOppgaveTask.doTask(task)

        verify(exactly = 1) {
            oppgaveService.opprettOppgave(
                behandlingId = behandlingId,
                oppgavetype = Oppgavetype.GodkjenneVedtak,
                beskrivelse = beskrivelse,
                prioritet = OppgavePrioritet.HOY,
                nokkelord = nokkelord,
            )
        }
    }

    @Test
    fun `skal kunne kjøre eldre tasker uten nokkelord`() {
        val task =
            Task(
                type = OpprettOppgaveTask.TYPE,
                payload = """{"behandlingId":"$behandlingId","oppgavetype":"GodkjenneVedtak"}""",
                properties = Properties(),
            )

        opprettOppgaveTask.doTask(task)

        verify(exactly = 1) {
            oppgaveService.opprettOppgave(
                behandlingId = behandlingId,
                oppgavetype = Oppgavetype.GodkjenneVedtak,
                nokkelord = emptySet(),
            )
        }
    }

    private fun verifyOpprettOppgaveMedLagringKall(opprettOppgaveKall: Int) {
        verify(exactly = opprettOppgaveKall) { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) }
    }

    private fun verifyOpprettOppgaveUtenLagringKall() {
        verify(exactly = 0) { oppgaveService.opprettOppgaveUtenÅLagreIRepository(any(), any(), any(), any(), any()) }
    }
}
