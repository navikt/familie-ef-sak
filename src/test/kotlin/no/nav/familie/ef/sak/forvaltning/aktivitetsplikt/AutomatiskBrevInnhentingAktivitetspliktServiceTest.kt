package no.nav.familie.ef.sak.forvaltning.aktivitetsplikt

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.MappeDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

internal class AutomatiskBrevInnhentingAktivitetspliktServiceTest {
    val taskService = mockk<TaskService>()
    val oppgaveService = mockk<OppgaveService>()

    private val automatiskBrevInnhentingAktivitetspliktService =
        AutomatiskBrevInnhentingAktivitetspliktService(taskService, oppgaveService)

    @BeforeEach
    fun setUp() {
        every { oppgaveService.finnMapper(any<String>()) } returns listOf(MappeDto(1, "64 Utdanning", "4489"))
    }

    val gjeldendeFrist = "2025-05-17"

    @Test
    fun `Skal opprette tasks for oppgaver`() {
        val taskSlots = mutableListOf<Task>()

        val oppgaver = listOf(oppgave(), oppgave(), oppgave(), oppgave(), oppgave())

        every { taskService.finnTaskMedPayloadOgType(any(), SendAktivitetspliktBrevTilIverksettTask.TYPE) } returns null
        every { taskService.save(capture(taskSlots)) } returns mockk()
        every { oppgaveService.hentOppgaver(any()) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = oppgaver.size.toLong(),
                oppgaver = oppgaver,
            )

        automatiskBrevInnhentingAktivitetspliktService.opprettTasks(
            liveRun = true,
            taskLimit = 5,
        )

        verifyOppgaveRequest(5, gjeldendeFrist)
        verify(exactly = oppgaver.size) { taskService.save(any()) }
        assertThat(taskSlots.size).isEqualTo(oppgaver.size)
        assertThat(taskSlots.all { it.type === SendAktivitetspliktBrevTilIverksettTask.TYPE }).isTrue
    }

    @Test
    fun `Skal ikke opprette tasks for oppgaver dersom liverun er false`() {
        val taskSlots = mutableListOf<Task>()

        val oppgaver = listOf(oppgave(), oppgave(), oppgave(), oppgave(), oppgave())

        every { taskService.save(capture(taskSlots)) } returns mockk()
        every { oppgaveService.hentOppgaver(any()) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = oppgaver.size.toLong(),
                oppgaver = oppgaver,
            )

        automatiskBrevInnhentingAktivitetspliktService.opprettTasks(
            liveRun = false,
            taskLimit = 5,
        )

        verifyOppgaveRequest(5, gjeldendeFrist)
        verify(exactly = 0) { taskService.save(any()) }
        assertThat(taskSlots.isEmpty()).isTrue
    }

    @Test
    fun `Skal ikke opprette tasks for oppgaver dersom oppgaver har tilordnetRessurs`() {
        val taskSlots = mutableListOf<Task>()

        val oppgaver = listOf(oppgave(tilordnetRessurs = "SaksbehandlerA"), oppgave(tilordnetRessurs = "SaksbehandlerB"), oppgave(), oppgave(), oppgave())

        every { taskService.finnTaskMedPayloadOgType(any(), SendAktivitetspliktBrevTilIverksettTask.TYPE) } returns null
        every { taskService.save(capture(taskSlots)) } returns mockk()
        every { oppgaveService.hentOppgaver(any()) } returns
            FinnOppgaveResponseDto(
                antallTreffTotalt = oppgaver.size.toLong(),
                oppgaver = oppgaver,
            )

        automatiskBrevInnhentingAktivitetspliktService.opprettTasks(
            liveRun = true,
            taskLimit = 5,
        )

        verifyOppgaveRequest(5, gjeldendeFrist)
        verify(exactly = 3) { taskService.save(any()) }
        assertThat(taskSlots.size).isEqualTo(3)
    }

    @Test
    fun `Skal hente oppgaver med frist 17-05-2024`() {
        val finnOppgaveRequestSlot = slot<FinnOppgaveRequest>()

        every { oppgaveService.hentOppgaver(capture(finnOppgaveRequestSlot)) } returns
            FinnOppgaveResponseDto(
                0,
                oppgaver = emptyList(),
            )

        automatiskBrevInnhentingAktivitetspliktService.opprettTasks(
            true,
            10,
        )

        verifyOppgaveRequest(10, gjeldendeFrist)
        assertThat(finnOppgaveRequestSlot.captured.fristFomDato).isEqualTo(LocalDate.parse(gjeldendeFrist))
        assertThat(finnOppgaveRequestSlot.captured.fristTomDato).isEqualTo(LocalDate.parse(gjeldendeFrist))
    }

    @Test
    fun `skal huske å oppdatere gjeldende frist innen juni neste år`() {
        if (YearMonth.now().month >= Month.JUNE) {
            assertThat(LocalDate.parse(gjeldendeFrist).year).isEqualTo(YearMonth.now().year)
        }
    }

    private fun verifyOppgaveRequest(
        taskLimit: Int,
        frist: String,
    ) {
        verify(exactly = 1) {
            oppgaveService.hentOppgaver(
                FinnOppgaveRequest(
                    tema = Tema.ENF,
                    fristFomDato = LocalDate.parse(frist),
                    fristTomDato = LocalDate.parse(frist),
                    mappeId = 1,
                    limit = taskLimit.toLong(),
                ),
            )
        }
    }

    companion object {
        private var oppgaveTeller = 1

        private fun oppgave(tilordnetRessurs: String? = null) = Oppgave(id = oppgaveTeller++.toLong(), tilordnetRessurs = tilordnetRessurs)
    }
}
