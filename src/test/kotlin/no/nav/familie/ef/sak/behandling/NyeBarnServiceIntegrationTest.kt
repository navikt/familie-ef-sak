package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.migrering.OpprettOppgaveForMigrertFødtBarnTask
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class NyeBarnServiceIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var nyeBarnService: NyeBarnService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var taskRepository: TaskRepository

    val ident = "123"

    @Test
    internal fun `skal kunne kalle på finnNyeBarnSidenGjeldendeBehandlingForPersonIdent flere ganger og kun opprette en task for OpprettOppgaveForMigrertFødtBarnTask`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = fagsakpersoner(setOf(ident)), migrert = true))
        behandlingRepository.insert(
            behandling(
                fagsak,
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.INNVILGET
            )
        )

        assertThat(nyeBarnService.finnNyeEllerTidligereFødteBarn(PersonIdent(ident)).nyeBarn).hasSize(2)
        assertThat(nyeBarnService.finnNyeEllerTidligereFødteBarn(PersonIdent(ident)).nyeBarn).hasSize(2)
        assertThat(taskRepository.findAll().filter { it.type == OpprettOppgaveForMigrertFødtBarnTask.TYPE }).hasSize(1)
    }

    @Test
    internal fun `skal ikke opprette oppgaver for fagsaker som ikke er migrert`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = fagsakpersoner(setOf(ident))))
        behandlingRepository.insert(
            behandling(
                fagsak,
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.INNVILGET
            )
        )

        assertThat(nyeBarnService.finnNyeEllerTidligereFødteBarn(PersonIdent(ident)).nyeBarn).hasSize(2)
        assertThat(taskRepository.findAll().filter { it.type == OpprettOppgaveForMigrertFødtBarnTask.TYPE }).isEmpty()
    }

    @Test
    fun `finnNyeEllerTidligereFødteBarn med et nytt barn i PDL siden barnetilsyn-behandling, forvent ett nytt barn`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = fagsakpersoner(setOf(ident)), migrert = true, stønadstype = StønadType.BARNETILSYN))
        behandlingRepository.insert(
            behandling(
                fagsak,
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.INNVILGET
            )
        )

        val barn = nyeBarnService.finnNyeEllerTidligereFødteBarn(PersonIdent(ident)).nyeBarn // PdlClient.hentBarn er mocket til å returnere 2 barn
        assertThat(barn).hasSize(2)
        assertThat(barn.all { it.stønadstype == StønadType.BARNETILSYN }).isTrue
    }

    /**
     * Denne kalles på av ef-personhendelse og man itererer over flere fagsaker på en person
     * Det kan være at en av disse fagsakene ikke har en iverksatt behandling ennå
     */
    @Test
    internal fun `finnNyeEllerTidligereFødteBarn skal ikke feile hvis det ikke finnes en behandling på fagsaken`() {
        testoppsettService.lagreFagsak(fagsak(identer = fagsakpersoner(setOf(ident)), migrert = true))

        val barn = nyeBarnService.finnNyeEllerTidligereFødteBarn(PersonIdent(ident)).nyeBarn

        assertThat(barn).isEmpty()
    }

    /**
     * Denne skal kaste feil fordi den er brukt fra frontend,
     * som kaller på denne hvis det finnes en tidligere iverksatt behandling
     * Og forventer då at det skal finnes en iverksatt/avslått behandling på fagsaken
     */
    @Test
    internal fun `finnNyeBarnSidenGjeldendeBehandlingForFagsak skal feile hvis den ikke finner en behandling på fagsak`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = fagsakpersoner(setOf(ident)), migrert = true))

        assertThatThrownBy {
            nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForFagsak(fagsak.id)
        }.hasMessageContaining("finner ikke behandling for")
    }
}
