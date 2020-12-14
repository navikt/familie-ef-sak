package no.nav.familie.ef.sak.no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandlingHistorikk
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingHistorikkRepository
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingHistorikk
import no.nav.familie.ef.sak.service.BehandlingHistorikkService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingHistorikkServiceTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingHistorikkService: BehandlingHistorikkService
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var behandlingHistorikkRepository: BehandlingHistorikkRepository
    @Autowired private lateinit var fagsakRepository: FagsakRepository

    @Test
    fun `lagre og hent behandling, forvent likhet`() {

        /** Lagre */
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val behandlingHistorikk = behandlingHistorikkRepository.insert(behandlingHistorikk(behandling))

        /** Hent */
        val innslag : BehandlingHistorikk = behandlingHistorikkService.finnBehandlingHistorikk(behandling.id).get(0)

        assertThat(innslag).isEqualTo(behandlingHistorikk)
    }
}



