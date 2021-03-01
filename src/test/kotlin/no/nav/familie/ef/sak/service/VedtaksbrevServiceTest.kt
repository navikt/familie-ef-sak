package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.integration.dto.familie.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

internal class VedtaksbrevServiceTest {

    private val taskRepository = mockk<TaskRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    private val journalpostClient = mockk<JournalpostClient>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val vedtaksbrevService = VedtaksbrevService(mockk(),
                                                        vedtaksbrevRepository,
                                                        behandlingService,
                                                        fagsakService,
                                                        mockk(),
                                                        journalpostClient,
                                                        arbeidsfordelingService)

    val fnr = "12345678901"
    val fagsak = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD,
                        søkerIdenter = setOf(FagsakPerson(ident = fnr)))
    val behandling = Behandling(fagsakId = fagsak.id,
                                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                status = BehandlingStatus.IVERKSETTER_VEDTAK,
                                steg = StegType.JOURNALFØR_VEDTAKSBREV,
                                resultat = BehandlingResultat.IKKE_SATT)
    val vedtaksbrev = Vedtaksbrev(behandling.id, mockk(), null, Fil("123".toByteArray()), Fil("123".toByteArray()))
    val arkiverDokumentRequestSlot = slot<ArkiverDokumentRequest>()

    @Test
    internal fun `skal journalføre vedtaksbrev`() {
        every { arbeidsfordelingService.hentNavEnhet(any()) } returns Arbeidsfordelingsenhet("4321", "enhetNavn")
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { vedtaksbrevRepository.findById(behandling.id) } returns Optional.of(vedtaksbrev)
        every {
            journalpostClient.arkiverDokument(capture(arkiverDokumentRequestSlot))
        } returns OppdaterJournalpostResponse("1234")
        every {
            taskRepository.save(any())
        } returns Task("", "", Properties())

        vedtaksbrevService.journalførVedtaksbrev(behandling.id)

        Assertions.assertThat(arkiverDokumentRequestSlot.captured.fnr).isEqualTo(fnr)
        Assertions.assertThat(arkiverDokumentRequestSlot.captured.fagsakId).isEqualTo(fagsak.eksternId.toString())
    }


}