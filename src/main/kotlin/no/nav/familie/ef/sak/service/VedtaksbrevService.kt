package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.BrevRequest
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.time.LocalDate
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
        val innvilgelseFra = LocalDate.now()
        val innvilgelseTil = LocalDate.now()
        val begrunnelseFomDatoInnvilgelse = "den måneden du ble separert"
        val brevdato = LocalDate.now()
        val belopOvergangsstonad = 13943

        val request = BrevRequest(navn = navn, ident = fagsak.hentAktivIdent(), innvilgelseFra = innvilgelseFra, innvilgelseTil = innvilgelseTil, begrunnelseFomDatoInnvilgelse = begrunnelseFomDatoInnvilgelse, brevdato = brevdato, belopOvergangsstonad = belopOvergangsstonad)

        val brev = brevRepository.insert(Vedtaksbrev(behandlingId = behandlingId,
                utkastBrevRequest = request,
                utkastPdf = brevClient.genererBrev("bokmaal",
                        "innvilgetVedtakMVP",
                        request)))
        return brev.utkastPdf
    }

    fun hentBrev(behandlingId: UUID): Vedtaksbrev {
        return brevRepository.findByIdOrThrow(behandlingId)
    }

}