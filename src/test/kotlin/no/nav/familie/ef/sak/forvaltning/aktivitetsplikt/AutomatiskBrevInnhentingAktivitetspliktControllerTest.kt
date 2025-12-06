package no.nav.familie.ef.sak.forvaltning.aktivitetsplikt

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal class AutomatiskBrevInnhentingAktivitetspliktControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var taskService: TaskService

    @BeforeEach
    internal fun setUp() {
        headers.setBearerAuth(lokalForvalterToken)
    }

    @Test
    internal fun `Skal opprette start task når liveRun er false`() {
        val respons = opprettTasks(liveRun = false, taskLimit = 10)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(taskService.findAll().any { it.type == StartUtsendingAvAktivitetspliktBrevTask.TYPE }).isTrue
    }

    @Test
    internal fun `Skal opprette start task når liveRun er true`() {
        val respons = opprettTasks(liveRun = true, taskLimit = 10)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(taskService.findAll().any { it.type == StartUtsendingAvAktivitetspliktBrevTask.TYPE }).isTrue
    }

    @Test
    internal fun `Skal kunne kjøre to ganger etter hverandre med samme payload`() {
        val respons = opprettTasks(liveRun = true, taskLimit = 10)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(taskService.findAll().any { it.type == StartUtsendingAvAktivitetspliktBrevTask.TYPE }).isTrue
        val responsAndreRunde = opprettTasks(liveRun = true, taskLimit = 10)
        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(taskService.findAll().count { it.type == StartUtsendingAvAktivitetspliktBrevTask.TYPE }).isEqualTo(2)
    }

    private fun opprettTasks(
        liveRun: Boolean = true,
        taskLimit: Int,
    ): ResponseEntity<Ressurs<Unit>> =
        restTemplate.exchange(
            localhost("/api/automatisk-brev-innhenting-aktivitetsplikt/opprett-tasks"),
            HttpMethod.POST,
            HttpEntity(AktivitetspliktRequest(liveRun = liveRun, taskLimit = taskLimit), headers),
        )
}
