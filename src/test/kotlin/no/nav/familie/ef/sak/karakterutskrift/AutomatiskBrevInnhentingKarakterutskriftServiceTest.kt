package no.nav.familie.ef.sak.karakterutskrift

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
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
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class AutomatiskBrevInnhentingKarakterutskriftServiceTest {

    val taskService = mockk<TaskService>()
    val oppgaveService = mockk<OppgaveService>()

    private val automatiskBrevInnhentingKarakterutskriftService =
        AutomatiskBrevInnhentingKarakterutskriftService(taskService, oppgaveService)

    @BeforeEach
    fun setUp() {
        every { oppgaveService.finnMapper(any<String>()) } returns listOf(MappeDto(1, "64 Utdanning", "4489"))
    }

    @Test
    fun `Skal opprette tasks for oppgaver`() {
        val taskSlots = mutableListOf<Task>()

        val oppgaver = listOf(oppgave(), oppgave(), oppgave(), oppgave(), oppgave())

        every { taskService.finnTaskMedPayloadOgType(any(), SendKarakterutskriftBrevTilIverksettTask.TYPE) } returns null
        every { taskService.save(capture(taskSlots)) } returns mockk()
        every { oppgaveService.hentOppgaver(any()) } returns FinnOppgaveResponseDto(
            antallTreffTotalt = oppgaver.size.toLong(),
            oppgaver = oppgaver,
        )

        automatiskBrevInnhentingKarakterutskriftService.opprettTasks(
            brevtype = FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE,
            liveRun = true,
            taskLimit = 5,
        )

        verifyOppgaveRequest(5, "2023-05-17")
        verify(exactly = oppgaver.size) { taskService.save(any()) }
        assertThat(taskSlots.size).isEqualTo(oppgaver.size)
        assertThat(taskSlots.all { it.type === SendKarakterutskriftBrevTilIverksettTask.TYPE }).isTrue
    }

    @Test
    fun `Skal ikke opprette tasks for oppgaver dersom liverun er false`() {
        val taskSlots = mutableListOf<Task>()

        val oppgaver = listOf(oppgave(), oppgave(), oppgave(), oppgave(), oppgave())

        every { taskService.save(capture(taskSlots)) } returns mockk()
        every { oppgaveService.hentOppgaver(any()) } returns FinnOppgaveResponseDto(
            antallTreffTotalt = oppgaver.size.toLong(),
            oppgaver = oppgaver,
        )

        automatiskBrevInnhentingKarakterutskriftService.opprettTasks(
            brevtype = FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE,
            liveRun = false,
            taskLimit = 5,
        )

        verifyOppgaveRequest(5, "2023-05-17")
        verify(exactly = 0) { taskService.save(any()) }
        assertThat(taskSlots.isEmpty()).isTrue
    }

    @Test
    fun `Skal hente oppgaver med frist 17-05-2023 dersom brevtype er INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE`() {
        val finnOppgaveRequestSlot = slot<FinnOppgaveRequest>()

        every { oppgaveService.hentOppgaver(capture(finnOppgaveRequestSlot)) } returns FinnOppgaveResponseDto(
            0,
            oppgaver = emptyList(),
        )

        automatiskBrevInnhentingKarakterutskriftService.opprettTasks(
            FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE,
            true,
            10,
        )

        verifyOppgaveRequest(10, "2023-05-17")
        assertThat(finnOppgaveRequestSlot.captured.fristFomDato).isEqualTo(LocalDate.parse("2023-05-17"))
        assertThat(finnOppgaveRequestSlot.captured.fristTomDato).isEqualTo(LocalDate.parse("2023-05-17"))
    }

    @Test
    fun `Skal hente oppgaver med frist 18-05-2023 dersom brevtype er INNHENTING_AV_KARAKTERUTSKRIFT_UTVIDET_PERIODE`() {
        val finnOppgaveRequestSlot = slot<FinnOppgaveRequest>()

        every { oppgaveService.hentOppgaver(capture(finnOppgaveRequestSlot)) } returns FinnOppgaveResponseDto(
            0,
            oppgaver = emptyList(),
        )

        automatiskBrevInnhentingKarakterutskriftService.opprettTasks(
            FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_UTVIDET_PERIODE,
            true,
            10,
        )

        verifyOppgaveRequest(10, "2023-05-18")
        assertThat(finnOppgaveRequestSlot.captured.fristFomDato).isEqualTo(LocalDate.parse("2023-05-18"))
        assertThat(finnOppgaveRequestSlot.captured.fristTomDato).isEqualTo(LocalDate.parse("2023-05-18"))
    }

    @Test
    fun `Skal feile opprettelse av tasks dersom brevtype er ugyldig`() {
        val finnOppgaveRequestSlot = slot<FinnOppgaveRequest>()

        every { oppgaveService.hentOppgaver(capture(finnOppgaveRequestSlot)) } returns FinnOppgaveResponseDto(
            0,
            oppgaver = emptyList(),
        )

        val feil = assertThrows<Feil> {
            automatiskBrevInnhentingKarakterutskriftService.opprettTasks(
                FrittståendeBrevType.BREV_OM_FORLENGET_SVARTID,
                true,
                10,
            )
        }
        assertThat(feil.message).contains("Skal ikke opprette automatiske innhentingsbrev for frittstående brev av type")
        verify(exactly = 0) { oppgaveService.hentOppgaver(any()) }
    }

    private fun verifyOppgaveRequest(taskLimit: Int, frist: String) {
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
        private fun oppgave() = Oppgave(id = oppgaveTeller++.toLong())
    }
}
