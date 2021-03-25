package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.BrevRequest
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.Fil
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.vedtaksbrev.BrevClient
import no.nav.familie.ef.sak.vedtaksbrev.BrevType
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.FilType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class VedtaksbrevService(private val brevClient: BrevClient,
                         private val brevRepository: VedtaksbrevRepository,
                         private val behandlingService: BehandlingService,
                         private val fagsakService: FagsakService,
                         private val personService: PersonService,
                         private val journalpostClient: JournalpostClient,
                         private val arbeidsfordelingService: ArbeidsfordelingService,
                         private val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

    fun lagBrevRequest(behandlingId: UUID): BrevRequest {
        val aktivIdent = behandlingService.hentAktivIdent(behandlingId)
        val person = personService.hentSøker(aktivIdent)
        val navn = person.navn.gjeldende().visningsnavn()
        val innvilgelseFra = LocalDate.now()
        val innvilgelseTil = LocalDate.now()
        val begrunnelseFomDatoInnvilgelse = "den måneden du ble separert"
        val brevdato = LocalDate.now()
        val belopOvergangsstonad = 13943
        val signaturSaksbehandler = SikkerhetContext.hentSaksbehandlerNavn()
        return BrevRequest(navn = navn,
                           ident = aktivIdent,
                           innvilgelseFra = innvilgelseFra,
                           innvilgelseTil = innvilgelseTil,
                           begrunnelseFomDatoInnvilgelse = begrunnelseFomDatoInnvilgelse,
                           brevdato = brevdato,
                           belopOvergangsstonad = belopOvergangsstonad,
                           signaturSaksbehandler = signaturSaksbehandler)
    }

    fun lagPdf(brevRequest: BrevRequest): ByteArray {
        return brevClient.genererBrev("bokmaal",
                                      "innvilgetVedtakMVP",
                                      brevRequest)
    }

    fun lagreBrevUtkast(behandlingId: UUID): Vedtaksbrev {
        val request = lagBrevRequest(behandlingId)
        val pdf = lagPdf(request)
        val brev = Vedtaksbrev(behandlingId, request, null, Fil(pdf), null)
        return brevRepository.insert(brev)
    }

    fun lagreEndeligBrev(behandlingId: UUID): Vedtaksbrev {
        val vedtaksbrev = brevRepository.findByIdOrThrow(behandlingId)
        val endeligRequest = vedtaksbrev.utkastBrevRequest.copy(signaturBeslutter = SikkerhetContext.hentSaksbehandlerNavn())
        return brevRepository.update(vedtaksbrev.copy(pdf = Fil(lagPdf(endeligRequest)), brevRequest = endeligRequest))
    }

    fun forhåndsvisBrev(behandlingId: UUID): ByteArray{
        return  lagPdf(lagBrevRequest(behandlingId))
    }

    fun hentBrev(behandlingId: UUID): Vedtaksbrev {
        return brevRepository.findByIdOrThrow(behandlingId)
    }

    fun journalførVedtaksbrev(behandlingId: UUID): String? {
        val fagsak = fagsakService.hentFaksakForBehandling(behandlingId)
        val ident = fagsak.hentAktivIdent();
        val vedtaksbrev = hentBrev(behandlingId)
        val dokumenter =
                listOf(Dokument(vedtaksbrev.pdf?.bytes ?: error("Mangler pdf ved journalføring av brev for bedhandling=$behandlingId"), FilType.PDFA, dokumentType = BrevType.VEDTAKSBREV.arkivMetadataType))
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhet(ident)

        return journalpostClient.arkiverDokument(ArkiverDokumentRequest(
                fnr = ident,
                forsøkFerdigstill = true,
                hoveddokumentvarianter = dokumenter,
                fagsakId = fagsak.eksternId.id.toString(),
                journalførendeEnhet = journalførendeEnhet?.enhetId
        )).journalpostId
    }

    fun distribuerVedtaksbrev(behandlingId: UUID, journpostId: String): String{
        return familieIntegrasjonerClient.distribuerBrev(journpostId)
    }
}