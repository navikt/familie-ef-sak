package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.BrevRequest
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.Fil
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

    fun lagBrevRequest(behandlingId: UUID): BrevRequest {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val person = personService.hentSøker(fagsak.hentAktivIdent())
        val navn = person.navn.gjeldende().visningsnavn()
        val innvilgelseFra = LocalDate.now()
        val innvilgelseTil = LocalDate.now()
        val begrunnelseFomDatoInnvilgelse = "den måneden du ble separert"
        val brevdato = LocalDate.now()
        val belopOvergangsstonad = 13943

        return BrevRequest(navn = navn,
                           ident = fagsak.hentAktivIdent(),
                           innvilgelseFra = innvilgelseFra,
                           innvilgelseTil = innvilgelseTil,
                           begrunnelseFomDatoInnvilgelse = begrunnelseFomDatoInnvilgelse,
                           brevdato = brevdato,
                           belopOvergangsstonad = belopOvergangsstonad)
    }

    fun lagPdf(brevRequest: BrevRequest): ByteArray {
        return brevClient.genererBrev("bokmaal",
                                      "innvilgetVedtakMVP",
                                      brevRequest)
    }

    fun lagreBrev(behandlingId: UUID) {
        val request = lagBrevRequest(behandlingId)
        val pdf = lagPdf(request)
        val brev = Vedtaksbrev(behandlingId, request, null, Fil(pdf), null)
        brevRepository.insert(brev)
    }

    fun forhåndsvisBrev(behandlingId: UUID): ByteArray{
        return  lagPdf(lagBrevRequest(behandlingId))
    }

    fun hentBrev(behandlingId: UUID): Vedtaksbrev {
        return brevRepository.findByIdOrThrow(behandlingId)
    }

}