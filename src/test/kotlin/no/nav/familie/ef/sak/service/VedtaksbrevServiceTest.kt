package no.nav.familie.ef.sak.service

import io.mockk.mockk
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.vedtaksbrev.BrevClient
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

internal class VedtaksbrevServiceTest : OppslagSpringRunnerTest() {


    @Autowired lateinit var vedtaksbrevService: VedtaksbrevService
    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var vedtaksbrevRepository: VedtaksbrevRepository

    val brevClientMock = mockk<BrevClient>()
    val behandlingServiceMock = mockk<BehandlingService>()
    val fagsakServiceMock = mockk<FagsakService>()
    val personServiceMock = mockk<PersonService>()
    val journalpostClientMock = mockk<JournalpostClient>()
    val arbeidsfordelingServiceMock = mockk<ArbeidsfordelingService>()
    val vedtaksbrevRepositoryMock = mockk<VedtaksbrevRepository>()
    val familieIntegrasjonerClientMock = mockk<FamilieIntegrasjonerClient>()

    private val fagsak = fagsak(setOf(FagsakPerson("")))
    private val behandling = behandling(fagsak)

    internal fun lagServiceMedMocker() = VedtaksbrevService(
            brevClientMock,
            vedtaksbrevRepositoryMock,
    )

    @BeforeEach
    internal fun setUp() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
    }


}