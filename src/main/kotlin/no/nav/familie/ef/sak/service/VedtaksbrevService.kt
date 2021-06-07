package no.nav.familie.ef.sak.service

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.Fil
import no.nav.familie.ef.sak.repository.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.vedtaksbrev.BrevClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VedtaksbrevService(private val brevClient: BrevClient,
                         private val brevRepository: VedtaksbrevRepository,
                         private val behandlingService: BehandlingService) {

    fun hentBrev(behandlingId: UUID) =
            brevRepository.findByIdOrThrow(behandlingId).beslutterPdf?.bytes
            ?: error("Mangler beslutterPdf for behandling=$behandlingId")

    fun lagBeslutterBrev(behandlingId: UUID): ByteArray {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.steg != StegType.BESLUTTE_VEDTAK || behandling.status != BehandlingStatus.FATTER_VEDTAK) {
            throw Feil("Behandling er i feil steg=${behandling.steg} status=${behandling.status}",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }

        val vedtaksbrev = brevRepository.findByIdOrThrow(behandlingId)
        val besluttervedtaksbrev = vedtaksbrev.copy(besluttersignatur = SikkerhetContext.hentSaksbehandlerNavn(strict = true))
        val beslutterPdf = Fil(brevClient.genererBrev(besluttervedtaksbrev))
        val besluttervedtaksbrevMedPdf = besluttervedtaksbrev.copy(beslutterPdf = beslutterPdf)
        brevRepository.update(besluttervedtaksbrevMedPdf)
        return beslutterPdf.bytes
    }

    fun lagSaksbehandlerBrev(behandlingId: UUID, brevrequest: JsonNode, brevmal: String): ByteArray {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.status.behandlingErLåstForVidereRedigering()) {
            throw Feil("Behandling er i feil steg=${behandling.steg} status=${behandling.status}",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }

        val saksbehandlersignatur = SikkerhetContext.hentSaksbehandlerNavn(strict = true)
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
