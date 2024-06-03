package no.nav.familie.ef.sak.forvaltning.aktivitetsplikt

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal class AutomatiskBrevInnhentingAktivitetspliktControllerTest : OppslagSpringRunnerTest() {
    @Autowired lateinit var taskService: TaskService

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalForvalterToken)
    }

    @Test
    internal fun `Skal ikke opprette tasks når liveRun er false`() {
        val respons = opprettTasks(liveRun = false, taskLimit = 10)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(taskService.findAll().none { it.type == SendAktivitetspliktBrevTilIverksettTask.TYPE }).isTrue
    }

    @Test
    internal fun `Skal opprette tasks når liveRun er true`() {
        val respons = opprettTasks(liveRun = true, taskLimit = 10)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(taskService.findAll().any { it.type == SendAktivitetspliktBrevTilIverksettTask.TYPE }).isTrue
    }

    @Test
    internal fun `Tasker skal ha unik callId`() {
        val respons = opprettTasks(liveRun = true, taskLimit = 10)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(taskService.findAll().any { it.type == SendAktivitetspliktBrevTilIverksettTask.TYPE }).isTrue
        assertThat(taskService.findAll().map { it.callId }).doesNotHaveDuplicates()
        assertThat(taskService.findAll().size > 1).isTrue
    }

    @Test
    internal fun `Skal ikke opprette tasker for oppgaver det allerede er opprettet for`() {
        val førsteRespons = opprettTasks(liveRun = true, taskLimit = 10)
        val antallTaskerEtterFørsteKjøring = taskService.findAll().any { it.type == SendAktivitetspliktBrevTilIverksettTask.TYPE }

        val andreRespons = opprettTasks(liveRun = true, taskLimit = 10)

        val antallTaskerEtterAndreKjøring = taskService.findAll().any { it.type == SendAktivitetspliktBrevTilIverksettTask.TYPE }

        assertThat(førsteRespons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(andreRespons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(antallTaskerEtterFørsteKjøring).isEqualTo(antallTaskerEtterAndreKjøring)
    }

    private fun opprettTasks(
        liveRun: Boolean = true,
        taskLimit: Int,
    ): ResponseEntity<Ressurs<Unit>> {
        return restTemplate.exchange(
            localhost("/api/automatisk-brev-innhenting-aktivitetsplikt/opprett-tasks"),
            HttpMethod.POST,
            HttpEntity(AktivitetspliktRequest(liveRun = liveRun, taskLimit = taskLimit), headers),
        )
    }
}
