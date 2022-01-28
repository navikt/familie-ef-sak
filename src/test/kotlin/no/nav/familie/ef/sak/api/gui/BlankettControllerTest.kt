package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.oppgave
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.UUID

class BlankettControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var oppgaveRepository: OppgaveRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Opprett blankett OK`() {
        val oppgaveId = 1L

        val respons: ResponseEntity<Ressurs<UUID>> = opprettBlankettBehandling(oppgaveId)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body.data).isNotNull
        assertThat(oppgaveRepository.findByGsakOppgaveId(1L)).isNotNull
    }

    @Test
    internal fun `Oppgave finnes allerede i db returner eksisterende bahandlingsid`() {

        val fagsak = testoppsettService.lagreFagsak(fagsak(setOf(FagsakPerson(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val oppgave = oppgaveRepository.insert(oppgave(behandling, false))
        val respons: ResponseEntity<Ressurs<UUID>> = opprettBlankettBehandling(oppgave.gsakOppgaveId)

        assertThat(respons.body.data).isEqualTo(behandling.id)
    }

    @Test
    internal fun `Oppgave er ikke knyttet til journalpost returner 500 IllegalStateException`() {

        val respons: ResponseEntity<Ressurs<UUID>> = opprettBlankettBehandling(11)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }


    private fun opprettBlankettBehandling(oppgaveId: Long): ResponseEntity<Ressurs<UUID>> {

        return restTemplate.exchange(localhost("/api/blankett/oppgave/$oppgaveId"),
                                     HttpMethod.POST,
                                     HttpEntity<Any>(headers))
    }
}