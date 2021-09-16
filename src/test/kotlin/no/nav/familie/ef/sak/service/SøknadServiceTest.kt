package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.repository.SøknadRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.Fagsak
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class SøknadServiceTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var søknadRepository: SøknadRepository
    @Autowired lateinit var søknadService: SøknadService

    @Test
    internal fun `skal kopiere søknadskjema til ny behandling`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val revurdering = behandlingRepository.insert(behandling(fagsak))

        val søknadsskjema = lagreSøknad(behandling, fagsak)
        val søknadsskjemaForRevurdering = kopierSøknadskjema(behandling, revurdering)

        assertThat(søknadsskjema).isEqualTo(søknadsskjemaForRevurdering)
    }

    @Test
    internal fun `skal opprette ny søknad til grunnlag som bruker den samme søknadsskjemaet`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val revurdering = behandlingRepository.insert(behandling(fagsak))

        lagreSøknad(behandling, fagsak)
        kopierSøknadskjema(behandling, revurdering)

        val søknad = søknadRepository.findByBehandlingId(behandling.id)!!
        val søknadForRevurdering = søknadRepository.findByBehandlingId(revurdering.id)!!

        assertThat(søknad.behandlingId).isNotEqualTo(søknadForRevurdering.behandlingId)
        assertThat(søknad.id).isNotEqualTo(søknadForRevurdering.id)
        assertThat(søknad.soknadsskjemaId).isEqualTo(søknadForRevurdering.soknadsskjemaId)
    }

    private fun kopierSøknadskjema(behandling: Behandling,
                                   revurdering: Behandling): SøknadsskjemaOvergangsstønad {
        søknadService.kopierSøknad(behandling.id, revurdering.id)
        return søknadService.hentOvergangsstønad(revurdering.id)
    }

    private fun lagreSøknad(behandling: Behandling,
                            fagsak: Fagsak): SøknadsskjemaOvergangsstønad {
        søknadService.lagreSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, behandling.id, fagsak.id, "1L")
        return søknadService.hentOvergangsstønad(behandling.id)
    }
}