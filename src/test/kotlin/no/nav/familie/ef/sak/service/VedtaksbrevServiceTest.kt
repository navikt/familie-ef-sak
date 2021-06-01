package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtaksbrev.BrevClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VedtaksbrevServiceTest {


    private val fagsak = fagsak(setOf(FagsakPerson("")))
    private val behandling = behandling(fagsak)

    val brevClient = mockk<BrevClient>()
    val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    val vedtaksbrevService = VedtaksbrevService(brevClient, vedtaksbrevRepository)

    val vedtaksbrev = Vedtaksbrev(behandling.id,
                                  "123",
                                  "malnavn",
                                  "Saksbehandler Signatur",
                                  null,
                                  null)


    @Test
    internal fun `skal legge på signatur og lage pdf ved lagBeslutterBrev`() {

        val vedtaksbrevSlot = slot<Vedtaksbrev>()

        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev
        every { brevClient.genererBrev(any()) } returns "123".toByteArray()
        every { vedtaksbrevRepository.update(capture(vedtaksbrevSlot)) } returns vedtaksbrev

        // TODO: Sjekke at signatur er null her?

        vedtaksbrevService.lagBeslutterBrev(behandling.id)
        Assertions.assertThat(vedtaksbrevSlot.captured.besluttersignatur).isNotBlank()






    }


}