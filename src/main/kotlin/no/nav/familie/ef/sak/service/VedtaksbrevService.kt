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
        // TODO validere at behandlig har rett status/steg?
        val vedtaksbrev = brevRepository.findByIdOrThrow(behandlingId)
        val besluttervedtaksbrev = vedtaksbrev.copy(besluttersignatur = SikkerhetContext.hentSaksbehandlerNavn())
        val beslutterPdf = Fil(brevClient.genererBrev(besluttervedtaksbrev))
        val besluttervedtaksbrevMedPdf = besluttervedtaksbrev.copy(beslutterPdf = beslutterPdf)
        return brevRepository.update(besluttervedtaksbrevMedPdf)
    }

    fun lagSaksbehandlerBrev(behandlingId: UUID, brevrequest: JsonNode, brevmal: String): ByteArray {
        // TODO validere at behandlig har rett status/steg?
        val saksbehandlersignatur = SikkerhetContext.hentSaksbehandlerNavn()
        val vedtaksbrev = when (brevRepository.existsById(behandlingId)) {
            true -> brevRepository.update(Vedtaksbrev(behandlingId,
                                                      brevrequest.toString(),
                                                      brevmal,
                                                      saksbehandlersignatur,
                                                      beslutterPdf = null))
            false -> brevRepository.insert(Vedtaksbrev(behandlingId,
                                                       brevrequest.toString(),
                                                       brevmal,
                                                       saksbehandlersignatur,
                                                       beslutterPdf = null))
        }

        return brevClient.genererBrev(vedtaksbrev)
    }

}