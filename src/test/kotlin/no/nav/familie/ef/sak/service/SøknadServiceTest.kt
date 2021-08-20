package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class SøknadServiceTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var søknadService: SøknadService

    @Test
    internal fun `skal kopiere søknad til ny behandling`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        søknadService.lagreSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, behandling.id, fagsak.id, "1L")
        val søknad = søknadService.hentOvergangsstønad(behandling.id)

        val revurdering = behandlingRepository.insert(behandling(fagsak))

        søknadService.kopierSøknad(behandling.id, revurdering.id)
        val søknadForRevurdering = søknadService.hentOvergangsstønad(revurdering.id)

        assertThat(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(søknad))
                .isEqualTo(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(søknadForRevurdering))
    }
}