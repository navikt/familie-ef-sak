package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import org.springframework.stereotype.Service
import java.util.*

@Deprecated("Vet ikke om dette skal/har blitt flyttet ti iverksetting")
@Service
class VedtaksbrevIverksettingService(
        private val brevRepository: VedtaksbrevRepository,
        private val fagsakService: FagsakService,
        private val journalpostClient: JournalpostClient,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

    private fun hentBrev(behandlingId: UUID): Vedtaksbrev {
        return brevRepository.findByIdOrThrow(behandlingId)
    }


    fun journalførVedtaksbrev(behandlingId: UUID): String? {
        val fagsak = fagsakService.hentFaksakForBehandling(behandlingId)
        val ident = fagsak.hentAktivIdent();
        val vedtaksbrev = hentBrev(behandlingId)
        val dokumenter =
                listOf(Dokument(vedtaksbrev.beslutterPdf?.bytes
                                ?: error("Mangler pdf ved journalføring av brev for bedhandling=$behandlingId"),
                                Filtype.PDFA,
                                dokumenttype = Dokumenttype.VEDTAKSBREV_OVERGANGSSTØNAD))
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhet(ident)

        return journalpostClient.arkiverDokument(ArkiverDokumentRequest(
                fnr = ident,
                forsøkFerdigstill = true,
                hoveddokumentvarianter = dokumenter,
                fagsakId = fagsak.eksternId.id.toString(),
                journalførendeEnhet = journalførendeEnhet?.enhetId,
                vedleggsdokumenter = emptyList()
        )).journalpostId
    }

    fun distribuerVedtaksbrev(behandlingId: UUID, journpostId: String): String {
        return familieIntegrasjonerClient.distribuerBrev(journpostId)
    }
}