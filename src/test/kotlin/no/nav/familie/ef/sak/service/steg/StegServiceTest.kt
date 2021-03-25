package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

internal class StegServiceTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var stegService: StegService
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun `skal håndtere en ny søknad`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.UTREDES))

        stegService.håndterVilkår(behandling)
    }

    @Test
    internal fun `skal feile håndtering av ny søknad hvis en behandling er ferdigstilt`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, steg = StegType.BEHANDLING_FERDIGSTILT))

        assertThrows<IllegalStateException> {
            stegService.håndterVilkår(behandling)
        }
    }

    @Test
    internal fun `skal feile håndtering av ny søknad hvis en behandling er sendt til beslutter`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, steg = StegType.BESLUTTE_VEDTAK))

        assertThrows<IllegalStateException> {
            stegService.håndterVilkår(behandling)
        }
    }
}