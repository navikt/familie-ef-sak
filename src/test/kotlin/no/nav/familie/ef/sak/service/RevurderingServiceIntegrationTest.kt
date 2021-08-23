package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.util.BrukerContextUtil
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class RevurderingServiceIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var revurderingService: RevurderingService
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var oppgaveService: OppgaveService
    @Autowired lateinit var søknadService: SøknadService

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun setUp() {
        BrukerContextUtil.mockBrukerContext("Heider")
        val personIdent = "123456789012"
        val identer = fagsakpersoner(setOf(personIdent))
        fagsak = fagsakRepository.insert(fagsak(identer = identer))
        behandling = behandlingRepository.insert(behandling(fagsak = fagsak, status = BehandlingStatus.FERDIGSTILT))
        lagreSøknad(behandling, fagsak)
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `Skal opprette revurdering`() {
        val opprettRevurderingManuelt = revurderingService.opprettRevurderingManuelt(fagsak.id)
        val hentEfOppgave = oppgaveService.hentEfOppgave(opprettRevurderingManuelt)
        val revurdering = behandlingRepository.findByIdOrThrow(hentEfOppgave!!.behandlingId)
        assertThat(revurdering.type).isEqualTo(BehandlingType.REVURDERING)
    }

    @Test
    internal fun `Skal ikke være mulig å opprette fagsak hvis siste behandling ikke er ferdig`() {
        val behandlingSomUtredes = behandling.copy(status = BehandlingStatus.UTREDES)
        behandlingRepository.update(behandlingSomUtredes)
        val exception =
                Assertions.assertThrows(IllegalStateException::class.java) { revurderingService.opprettRevurderingManuelt(fagsak.id) }
        assertThat(exception.message).isEqualTo("Revurdering må ha eksisterende iverksatt behandling")
    }


    private fun lagreSøknad(behandling: Behandling,
                            fagsak: Fagsak): SøknadsskjemaOvergangsstønad {
        søknadService.lagreSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, behandling.id, fagsak.id, "1L")
        return søknadService.hentOvergangsstønad(behandling.id)
    }
}