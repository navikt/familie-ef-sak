package no.nav.familie.ef.sak.vedtak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.domene.Fil
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksbrev
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VedtaksbrevService(private val brevClient: BrevClient,
                         private val brevRepository: VedtaksbrevRepository,
                         private val behandlingService: BehandlingService) {

    fun hentBeslutterbrevEllerRekonstruerSaksbehandlerBrev(behandlingId: UUID): ByteArray {
        val vedtaksbrev = brevRepository.findByIdOrThrow(behandlingId)
        return if (vedtaksbrev.beslutterPdf != null) {
            vedtaksbrev.beslutterPdf.bytes
        } else {
            brevClient.genererBrev(vedtaksbrev)
        }
    }

    fun lagBeslutterBrev(behandlingId: UUID): ByteArray {
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.steg != StegType.BESLUTTE_VEDTAK || behandling.status != BehandlingStatus.FATTER_VEDTAK) {
            throw Feil("Behandling er i feil steg=${behandling.steg} status=${behandling.status}",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }

        val vedtaksbrev = brevRepository.findByIdOrThrow(behandlingId)
        val besluttersignatur = SikkerhetContext.hentSaksbehandlerNavn(strict = true)
        val besluttervedtaksbrev = vedtaksbrev.copy(besluttersignatur = besluttersignatur)

        validerBeslutterIkkeErLikSaksbehandler(vedtaksbrev, besluttersignatur)

        val beslutterPdf = Fil(brevClient.genererBrev(besluttervedtaksbrev))
        val besluttervedtaksbrevMedPdf = besluttervedtaksbrev.copy(beslutterPdf = beslutterPdf)
        brevRepository.update(besluttervedtaksbrevMedPdf)
        return beslutterPdf.bytes
    }

    private fun validerBeslutterIkkeErLikSaksbehandler(vedtaksbrev: Vedtaksbrev,
                                                       besluttersignatur: String) {
        if (vedtaksbrev.saksbehandlersignatur == besluttersignatur) {
            throw Feil(message = "Beslutter er lik behandler",
                       frontendFeilmelding = "Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter")
        }
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
