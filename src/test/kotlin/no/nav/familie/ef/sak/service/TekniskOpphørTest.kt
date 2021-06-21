package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.service.TekniskOpphørService
import no.nav.familie.ef.sak.task.PollStatusTekniskOpphør
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class TekniskOpphørTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var tekniskOpphørService: TekniskOpphørService
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var taskRepository : TaskRepository
    @Autowired lateinit var pollStatusTekniskOpphør: PollStatusTekniskOpphør

    @Test
    internal fun `skal iverksette teknisk opphør og vente på status uten å kasta exceptions`() {
        val ident = "1234"
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(ident = ident))))
        behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))

        tekniskOpphørService.håndterTeknisktOpphør(PersonIdent(ident))
        val task = taskRepository.findAll().first()
        assertThat(task.type).isEqualTo(PollStatusTekniskOpphør.TYPE)
        task.let { pollStatusTekniskOpphør.doTask(it)}

    }


}