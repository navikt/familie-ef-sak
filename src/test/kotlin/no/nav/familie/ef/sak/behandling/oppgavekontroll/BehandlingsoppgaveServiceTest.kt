package no.nav.familie.ef.sak.behandling.oppgavekontroll

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.vedtak.NullstillVedtakService
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class BehandlingsoppgaveServiceTest(){


    private val taskService = mockk<TaskService>(relaxed = true)
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService= mockk()
    private val oppgaveService: OppgaveService = mockk()

    val behandlingsoppgaveService = BehandlingsoppgaveService(taskService, behandlingService, fagsakService, oppgaveService)

    @Test
    internal fun `Skal returnere riktig antall uten oppgave når vi finner to`() {
      //  every {  }

        val antallÅpneBehandlingerUtenOppgave = behandlingsoppgaveService.antallÅpneBehandlingerUtenOppgave()


        Assertions.assertThat(antallÅpneBehandlingerUtenOppgave).isEqualTo(2)


    }

}