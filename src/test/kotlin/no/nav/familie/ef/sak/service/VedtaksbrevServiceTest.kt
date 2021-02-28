package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.lang.IllegalStateException

internal class VedtaksbrevServiceTest: OppslagSpringRunnerTest() {


    @Autowired lateinit var vedtaksbrevService: VedtaksbrevService
    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository

    private val fagsak = fagsak(setOf(FagsakPerson("")))
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `skal ikke kunne lage endelig brev hvis utkast ikke finnes`(){
        assertThrows<IllegalStateException> { vedtaksbrevService.lagreEndeligBrev(behandlingId = behandling.id) }
    }

    @Test
    fun `skal lage endelig brev basert på utkast`(){
        val utkast = vedtaksbrevService.lagreBrevUtkast(behandlingId = behandling.id)
        val endelig = vedtaksbrevService.lagreEndeligBrev(behandlingId = behandling.id)

        Assertions.assertThat(utkast).usingRecursiveComparison().ignoringFields("brevRequest", "pdf").isEqualTo(endelig)
        Assertions.assertThat(utkast.utkastBrevRequest).usingRecursiveComparison().ignoringFields("signaturBeslutter").isEqualTo(endelig.brevRequest)
    }

}