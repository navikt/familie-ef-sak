package no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.NyeBarnService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.migrering.OpprettOppgaveForMigrertFødtBarnTask
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class NyeBarnServiceIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var nyeBarnService: NyeBarnService
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var taskRepository: TaskRepository

    val ident = "123"

    @Test
    internal fun `skal kunne kalle på finnNyeBarnSidenGjeldendeBehandlingForPersonIdent flere ganger og kun opprette en task for OpprettOppgaveForMigrertFødtBarnTask`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = fagsakpersoner(setOf(ident)), migrert = true))
        behandlingRepository.insert(behandling(fagsak,
                                               status = BehandlingStatus.FERDIGSTILT,
                                               resultat = BehandlingResultat.INNVILGET))

        assertThat(nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent(ident))).hasSize(2)
        assertThat(nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent(ident))).hasSize(2)
        assertThat(taskRepository.findAll().filter { it.type == OpprettOppgaveForMigrertFødtBarnTask.TYPE }).hasSize(1)
    }

    @Test
    internal fun `skal ikke opprette oppgaver for fagsaker som ikke er migrert`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = fagsakpersoner(setOf(ident))))
        behandlingRepository.insert(behandling(fagsak,
                                               status = BehandlingStatus.FERDIGSTILT,
                                               resultat = BehandlingResultat.INNVILGET))

        assertThat(nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent(ident))).hasSize(2)
        assertThat(taskRepository.findAll().filter { it.type == OpprettOppgaveForMigrertFødtBarnTask.TYPE }).isEmpty()
    }
}