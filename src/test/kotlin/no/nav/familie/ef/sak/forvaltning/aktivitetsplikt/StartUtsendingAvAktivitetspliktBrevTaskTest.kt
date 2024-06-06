package no.nav.familie.ef.sak.no.nav.familie.ef.sak.forvaltning.aktivitetsplikt

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.forvaltning.aktivitetsplikt.AktivitetspliktRequest
import no.nav.familie.ef.sak.forvaltning.aktivitetsplikt.SendAktivitetspliktBrevTilIverksettTask
import no.nav.familie.ef.sak.forvaltning.aktivitetsplikt.StartUtsendingAvAktivitetspliktBrevTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class StartUtsendingAvAktivitetspliktBrevTaskTest : OppslagSpringRunnerTest() {
    @Autowired lateinit var taskService: TaskService

    @Autowired lateinit var startUtsendingAvAktivitetspliktBrevTask: StartUtsendingAvAktivitetspliktBrevTask

    @Test
    internal fun `Skal ikke opprette tasks når liveRun er false`() {
        val task = opprettTask(liveRun = false, taskLimit = 10)
        startUtsendingAvAktivitetspliktBrevTask.doTask(task)
        assertThat(taskService.findAll().none { it.type == SendAktivitetspliktBrevTilIverksettTask.TYPE }).isTrue
    }

    @Test
    internal fun `Skal opprette tasks når liveRun er true`() {
        val task = opprettTask(liveRun = true, taskLimit = 10)
        startUtsendingAvAktivitetspliktBrevTask.doTask(task)
        assertThat(taskService.findAll().any { it.type == SendAktivitetspliktBrevTilIverksettTask.TYPE }).isTrue
    }

    @Test
    internal fun `Tasker skal ha unik callId`() {
        val task = opprettTask(liveRun = true, taskLimit = 10)
        startUtsendingAvAktivitetspliktBrevTask.doTask(task)
        assertThat(taskService.findAll().any { it.type == SendAktivitetspliktBrevTilIverksettTask.TYPE }).isTrue
        assertThat(taskService.findAll().map { it.callId }).doesNotHaveDuplicates()
        assertThat(taskService.findAll().size > 1).isTrue
    }

    @Test
    internal fun `Skal ikke opprette tasker for oppgaver det allerede er opprettet for`() {
        val førsteTask = opprettTask(liveRun = true, taskLimit = 10)
        startUtsendingAvAktivitetspliktBrevTask.doTask(førsteTask)
        val antallTaskerEtterFørsteKjøring = taskService.findAll().any { it.type == SendAktivitetspliktBrevTilIverksettTask.TYPE }

        val andreTask = opprettTask(liveRun = true, taskLimit = 10)
        startUtsendingAvAktivitetspliktBrevTask.doTask(andreTask)

        val antallTaskerEtterAndreKjøring = taskService.findAll().any { it.type == SendAktivitetspliktBrevTilIverksettTask.TYPE }

        assertThat(antallTaskerEtterFørsteKjøring).isEqualTo(antallTaskerEtterAndreKjøring)
    }

    private fun opprettTask(
        liveRun: Boolean = true,
        taskLimit: Int,
    ): Task = StartUtsendingAvAktivitetspliktBrevTask.opprettTask(AktivitetspliktRequest(liveRun = liveRun, taskLimit = taskLimit))
}
