package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.findByIdOrThrow
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
    @Autowired private lateinit var vedtaksbrevRepository: VedtaksbrevRepository

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

    @Test
    fun `utkast skal lagres i databasen`(){
        val utkast = vedtaksbrevService.lagreBrevUtkast(behandlingId = behandling.id)
        val forventetRequest = vedtaksbrevService.lagBrevRequest(behandlingId = behandling.id)

        Assertions.assertThat(utkast).isEqualTo(vedtaksbrevRepository.findByIdOrThrow(behandling.id))
        Assertions.assertThat(forventetRequest).isEqualTo(vedtaksbrevRepository.findByIdOrThrow(behandling.id).utkastBrevRequest)
        Assertions.assertThat(vedtaksbrevRepository.findByIdOrThrow(behandling.id).utkastPdf).isNotNull
    }

    @Test
    fun `endelig brev skal lagres i databasen`(){
        vedtaksbrevService.lagreBrevUtkast(behandlingId = behandling.id)
        val endelig = vedtaksbrevService.lagreEndeligBrev(behandlingId = behandling.id)

        Assertions.assertThat(endelig).isEqualTo(vedtaksbrevRepository.findByIdOrThrow(behandling.id))
        Assertions.assertThat(vedtaksbrevRepository.findByIdOrThrow(behandling.id).pdf).isNotNull
    }

    @Test
    fun `endelig brev skal ikke lagres ved generering av utkast`(){
        vedtaksbrevService.lagreBrevUtkast(behandlingId = behandling.id)

        Assertions.assertThat(vedtaksbrevRepository.findByIdOrThrow(behandling.id).brevRequest).isNull()
        Assertions.assertThat(vedtaksbrevRepository.findByIdOrThrow(behandling.id).pdf).isNull()
    }

}