package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.BrevRequest
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import no.nav.familie.ef.sak.service.steg.StegType
import org.springframework.stereotype.Service
import java.util.*

@Service
class VedtaksbrevService(private val brevClient: BrevClient,
                         private val brevRepository: VedtaksbrevRepository,
                         private val behandlingService: BehandlingService,
                         private val fagsakService: FagsakService,
                         private val personService: PersonService) {

    fun lagBrev(behandlingId: UUID): ByteArray {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val person = personService.hentSøker(fagsak.hentAktivIdent())
        val navn = person.navn.gjeldende().visningsnavn()

        val request = BrevRequest(navn = navn, ident = fagsak.hentAktivIdent())

        val brev = brevRepository.insert(Vedtaksbrev(behandlingId = behandlingId,
                                                     brevRequest = request,
                                                     steg = behandling.steg,
                                                     pdf = brevClient.genererBrev("bokmaal",
                                                                                  "testDokument",
                                                                                  request)))
        return brev.pdf
    }

    fun hentBrev(behandlingId: UUID): Vedtaksbrev? {
        return brevRepository.findByBehandlingId(behandlingId).first()
    }

    fun hentBrevForTotrinnskontroll(behandlingId: UUID): Vedtaksbrev? {
        return brevRepository.findByBehandlingId(behandlingId).firstOrNull { it.steg === StegType.BESLUTTE_VEDTAK }
    }

}