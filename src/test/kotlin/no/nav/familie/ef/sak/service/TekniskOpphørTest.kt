package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service

import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.service.TekniskOpphørService
import no.nav.familie.ef.sak.task.PollStatusTekniskOpphør
import no.nav.familie.kontrakter.ef.iverksett.IverksettStatus
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class TekniskOpphørTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var tekniskOpphørService: TekniskOpphørService
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var taskRepository : TaskRepository
    @Autowired lateinit var pollStatusTekniskOpphør: PollStatusTekniskOpphør
    @Autowired lateinit var iverksettClient: IverksettClient

    @BeforeEach
    internal fun setUp() {
        mockBrukerContext("saksbehandler")
        every { iverksettClient.hentStatus(any()) } returns IverksettStatus.OK_MOT_OPPDRAG
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `skal iverksette teknisk opphør og vente på status uten å kasta exceptions`() {
        val ident = "1234"
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(ident = ident))))
        behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))

        tekniskOpphørService.håndterTeknisktOpphør(PersonIdent(ident))

        val task = taskRepository.findAll().first()
        assertThat(task.type).isEqualTo(PollStatusTekniskOpphør.TYPE)
        clearBrukerContext() // må kjøre task i context av system
        pollStatusTekniskOpphør.doTask(task)
    }


}