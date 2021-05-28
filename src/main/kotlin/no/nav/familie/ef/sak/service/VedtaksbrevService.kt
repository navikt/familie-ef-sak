package no.nav.familie.ef.sak.service

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.Fil
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.vedtaksbrev.BrevClient
import org.springframework.stereotype.Service
import java.util.*

@Service
class VedtaksbrevService(private val brevClient: BrevClient,
                         private val brevRepository: VedtaksbrevRepository) {


    fun lagBeslutterBrev(behandlingId: UUID): Vedtaksbrev {
        val vedtaksbrev = brevRepository.findByIdOrThrow(behandlingId)
        val besluttersignatur = SikkerhetContext.hentSaksbehandlerNavn()
        val beslutterPdf = Fil(brevClient.genererBeslutterbrev("bokmaal",
                                                               vedtaksbrev.brevmal,
                                                               vedtaksbrev.saksbehandlerBrevrequest,
                                                               besluttersignatur))
        val besluttervedtaksbrev = vedtaksbrev.copy(beslutterPdf = beslutterPdf,
                                                    besluttersignatur = besluttersignatur)
        return brevRepository.update(besluttervedtaksbrev)
    }

    fun lagBrev(behandlingId: UUID, brevRequest: JsonNode, brevMal: String): ByteArray {
        return brevClient.genererBrev("bokmaal",
                                      brevMal,
                                      brevRequest)
    }



    fun lagreBrevrequest(behandlingId: UUID, brevrequest: JsonNode, brevmal: String, pdf: Fil?) {
        // TODO validere at dette ikke er totrinn?
        val saksbehandlersignatur = SikkerhetContext.hentSaksbehandlerNavn()
        when (brevRepository.existsById(behandlingId)) {
            true -> brevRepository.update(Vedtaksbrev(behandlingId, brevrequest.toString(), brevmal, pdf, saksbehandlersignatur))
            false -> brevRepository.insert(Vedtaksbrev(behandlingId, brevrequest.toString(), brevmal, pdf, saksbehandlersignatur))
        }
    }
}