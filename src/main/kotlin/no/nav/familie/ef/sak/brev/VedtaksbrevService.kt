package no.nav.familie.ef.sak.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.brev.domain.FRITEKST
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.brev.domain.tilDto
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevRequestDto
import no.nav.familie.ef.sak.brev.dto.VedtaksbrevFritekstDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VedtaksbrevService(private val brevClient: BrevClient,
                         private val brevRepository: VedtaksbrevRepository,
                         private val behandlingService: BehandlingService,
                         private val personopplysningerService: PersonopplysningerService,
                         private val brevsignaturService: BrevsignaturService,
                         private val fagsakService: FagsakService) {

    fun hentBeslutterbrevEllerRekonstruerSaksbehandlerBrev(behandlingId: UUID): ByteArray {
        val vedtaksbrev = brevRepository.findByIdOrThrow(behandlingId)
        return if (vedtaksbrev.beslutterPdf != null) {
            vedtaksbrev.beslutterPdf.bytes
        } else {
            brevClient.genererBrev(vedtaksbrev.tilDto())
        }
    }

    fun lagSaksbehandlerSanitybrev(behandlingId: UUID, brevrequest: JsonNode, brevmal: String): ByteArray {
        val behandling = behandlingService.hentBehandling(behandlingId)
        validerRedigerbarBehandling(behandling)

        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val saksbehandlersignatur = brevsignaturService.lagSignaturMedEnhet(fagsak)

        val vedtaksbrev = lagreEllerOppdaterVedtaksbrev(
            behandlingId,
            brevrequest.toString(),
            brevmal,
            saksbehandlersignatur.navn,
            saksbehandlersignatur.enhet
        )

        return brevClient.genererBrev(vedtaksbrev.tilDto())
    }

    private fun lagreEllerOppdaterVedtaksbrev(behandlingId: UUID,
                                              brevrequest: String,
                                              brevmal: String,
                                              saksbehandlersignatur: String,
                                              enhet: String): Vedtaksbrev {
        val vedtaksbrev = Vedtaksbrev(behandlingId,
                                      brevrequest,
                                      brevmal,
                                      saksbehandlersignatur,
                                      beslutterPdf = null,
                                      enhet = enhet)
        return when (brevRepository.existsById(behandlingId)) {
            true -> brevRepository.update(vedtaksbrev)
            false -> brevRepository.insert(vedtaksbrev)
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

        val beslutterPdf = Fil(brevClient.genererBrev(besluttervedtaksbrev.tilDto()))
        val besluttervedtaksbrevMedPdf = besluttervedtaksbrev.copy(beslutterPdf = beslutterPdf)
        brevRepository.update(besluttervedtaksbrevMedPdf)
        return beslutterPdf.bytes
    }

    fun lagSaksbehandlerFritekstbrev(frittståendeBrevDto: VedtaksbrevFritekstDto): ByteArray {
        val behandling = behandlingService.hentBehandling(frittståendeBrevDto.behandlingId)
        validerRedigerbarBehandling(behandling)
        val ident = behandlingService.hentAktivIdent(frittståendeBrevDto.behandlingId)
        val navn = personopplysningerService.hentGjeldeneNavn(listOf(ident))
        val request = FrittståendeBrevRequestDto(overskrift = frittståendeBrevDto.overskrift,
                                                 avsnitt = frittståendeBrevDto.avsnitt,
                                                 personIdent = ident,
                                                 navn = navn[ident]!!)

        val signaturMedEnhet = brevsignaturService.lagSignaturMedEnhet(fagsakService.hentFagsak(behandling.fagsakId))
        val vedtaksbrev = lagreEllerOppdaterVedtaksbrev(behandlingId = frittståendeBrevDto.behandlingId,
                                                        brevrequest = objectMapper.writeValueAsString(request),
                                                        brevmal = FRITEKST,
                                                        saksbehandlersignatur = signaturMedEnhet.navn,
                                                        enhet = signaturMedEnhet.enhet)

        return brevClient.genererBrev(vedtaksbrev = vedtaksbrev.tilDto())
    }

    private fun validerRedigerbarBehandling(behandling: Behandling) {
        if (behandling.status.behandlingErLåstForVidereRedigering()) {
            throw Feil("Behandling er i feil steg=${behandling.steg} status=${behandling.status}",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerBeslutterIkkeErLikSaksbehandler(vedtaksbrev: Vedtaksbrev,
                                                       besluttersignatur: String) {
        if (vedtaksbrev.saksbehandlersignatur == besluttersignatur) {
            throw Feil(message = "Beslutter er lik behandler",
                       frontendFeilmelding = "Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter")
        }
    }


}
