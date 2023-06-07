package no.nav.familie.ef.sak.karakterutskrift

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
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

internal class AutomatiskBrevInnhentingKarakterutskriftControllerTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var taskService: TaskService

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal ikke opprette tasks når liveRun er false`() {
        val respons = opprettTasks(liveRun = false)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(taskService.findAll().none { it.type == SendKarakterutskriftBrevTilIverksettTask.TYPE }).isTrue
    }

    @Test
    internal fun `Skal opprette tasks når liveRun er true`() {
        val respons = opprettTasks(liveRun = true)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(taskService.findAll().any { it.type == SendKarakterutskriftBrevTilIverksettTask.TYPE }).isTrue
    }

    @Test
    internal fun `Tasker skal ha unik callId`() {
        val respons = opprettTasks(liveRun = true)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(taskService.findAll().any { it.type == SendKarakterutskriftBrevTilIverksettTask.TYPE }).isTrue
        assertThat(taskService.findAll().map { it.callId }).doesNotHaveDuplicates()
        assertThat(taskService.findAll().size > 1).isTrue
    }

    @Test
    internal fun `Skal ikke opprette tasker for oppgaver det allerede er opprettet for`() {
        val førsteRespons = opprettTasks(liveRun = true)
        val antallTaskerEtterFørsteKjøring = taskService.findAll().any { it.type == SendKarakterutskriftBrevTilIverksettTask.TYPE }

        val andreRespons = opprettTasks(liveRun = true)

        val antallTaskerEtterAndreKjøring = taskService.findAll().any { it.type == SendKarakterutskriftBrevTilIverksettTask.TYPE }

        assertThat(førsteRespons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(andreRespons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(antallTaskerEtterFørsteKjøring).isEqualTo(antallTaskerEtterAndreKjøring)
    }

    private fun opprettTasks(
        liveRun: Boolean = true,
        brevtype: FrittståendeBrevType = FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE,
    ): ResponseEntity<Ressurs<Unit>> {
        return restTemplate.exchange(
            localhost("/api/automatisk-brev-innhenting-karakterutskrift/opprett-tasks"),
            HttpMethod.POST,
            HttpEntity(KarakterutskriftRequest(liveRun = liveRun, frittståendeBrevType = brevtype), headers),
        )
    }
}
