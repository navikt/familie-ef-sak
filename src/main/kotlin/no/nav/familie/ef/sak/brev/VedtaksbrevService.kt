package no.nav.familie.ef.sak.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.brev.domain.FRITEKST
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.brev.domain.VedtaksbrevKonstanter.IKKE_SATT_IDENT_PÅ_GAMLE_VEDTAKSBREV
import no.nav.familie.ef.sak.brev.domain.tilDto
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevRequestDto
import no.nav.familie.ef.sak.brev.dto.VedtaksbrevFritekstDto
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
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
                         private val personopplysningerService: PersonopplysningerService,
                         private val brevsignaturService: BrevsignaturService) {

    fun hentBeslutterbrevEllerRekonstruerSaksbehandlerBrev(behandlingId: UUID): ByteArray {
        val vedtaksbrev = brevRepository.findByIdOrThrow(behandlingId)
        return if (vedtaksbrev.beslutterPdf != null) {
            vedtaksbrev.beslutterPdf.bytes
        } else {
            // Besluttersignatur er ikke laget ennå skjulBeslutterSignatur vil ikke ha noen betydning.
            brevClient.genererBrev(vedtaksbrev.tilDto(true))
        }
    }

    fun lagSaksbehandlerSanitybrev(saksbehandling: Saksbehandling, brevrequest: JsonNode, brevmal: String): ByteArray {
        validerRedigerbarBehandling(saksbehandling)

        val saksbehandlersignatur = brevsignaturService.lagSignaturMedEnhet(saksbehandling)

        val vedtaksbrev = lagreEllerOppdaterSaksbehandlerVedtaksbrev(
                saksbehandling.id,
                brevrequest.toString(),
                brevmal,
                saksbehandlersignatur.navn,
                saksbehandlersignatur.enhet
        )

        return brevClient.genererBrev(vedtaksbrev.tilDto(saksbehandlersignatur.skjulBeslutter))
    }

    private fun lagreEllerOppdaterSaksbehandlerVedtaksbrev(behandlingId: UUID,
                                                           brevrequest: String,
                                                           brevmal: String,
                                                           saksbehandlersignatur: String,
                                                           enhet: String): Vedtaksbrev {
        val vedtaksbrev = Vedtaksbrev(behandlingId = behandlingId,
                                      saksbehandlerBrevrequest = brevrequest,
                                      brevmal = brevmal,
                                      saksbehandlersignatur = saksbehandlersignatur,
                                      enhet = enhet,
                                      saksbehandlerident = SikkerhetContext.hentSaksbehandler(true))

        return when (brevRepository.existsById(behandlingId)) {
            true -> brevRepository.update(vedtaksbrev)
            false -> brevRepository.insert(vedtaksbrev)
        }
    }


    fun lagBeslutterBrev(saksbehandling: Saksbehandling): ByteArray {
        if (saksbehandling.steg != StegType.BESLUTTE_VEDTAK || saksbehandling.status != BehandlingStatus.FATTER_VEDTAK) {
            throw Feil("Behandling er i feil steg=${saksbehandling.steg} status=${saksbehandling.status}",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }

        val vedtaksbrev = brevRepository.findByIdOrThrow(saksbehandling.id)
        val signaturMedEnhet = brevsignaturService.lagSignaturMedEnhet(saksbehandling)
        val besluttervedtaksbrev = vedtaksbrev.copy(besluttersignatur = signaturMedEnhet.navn,
                                                    enhet = signaturMedEnhet.enhet,
                                                    beslutterident = SikkerhetContext.hentSaksbehandler(true))

        validerBeslutterIkkeErLikSaksbehandler(besluttervedtaksbrev)

        val beslutterPdf = Fil(brevClient.genererBrev(besluttervedtaksbrev.tilDto(signaturMedEnhet.skjulBeslutter)))
        val besluttervedtaksbrevMedPdf = besluttervedtaksbrev.copy(beslutterPdf = beslutterPdf)
        brevRepository.update(besluttervedtaksbrevMedPdf)
        return beslutterPdf.bytes
    }

    fun lagSaksbehandlerFritekstbrev(frittståendeBrevDto: VedtaksbrevFritekstDto, saksbehandling: Saksbehandling): ByteArray {
        validerRedigerbarBehandling(saksbehandling)
        val navn = personopplysningerService.hentGjeldeneNavn(listOf(saksbehandling.ident))
        val request = FrittståendeBrevRequestDto(overskrift = frittståendeBrevDto.overskrift,
                                                 avsnitt = frittståendeBrevDto.avsnitt,
                                                 personIdent = saksbehandling.ident,
                                                 navn = navn[saksbehandling.ident]!!)

        val signaturMedEnhet = brevsignaturService.lagSignaturMedEnhet(saksbehandling)
        val vedtaksbrev = lagreEllerOppdaterSaksbehandlerVedtaksbrev(behandlingId = frittståendeBrevDto.behandlingId,
                                                                     brevrequest = objectMapper.writeValueAsString(request),
                                                                     brevmal = FRITEKST,
                                                                     saksbehandlersignatur = signaturMedEnhet.navn,
                                                                     enhet = signaturMedEnhet.enhet)

        return brevClient.genererBrev(vedtaksbrev = vedtaksbrev.tilDto(signaturMedEnhet.skjulBeslutter))
    }

    private fun validerRedigerbarBehandling(behandling: Saksbehandling) {
        if (behandling.status.behandlingErLåstForVidereRedigering()) {
            throw Feil("Behandling er i feil steg=${behandling.steg} status=${behandling.status}",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerBeslutterIkkeErLikSaksbehandler(vedtaksbrev: Vedtaksbrev) {
        brukerfeilHvis(vedtaksbrev.beslutterident.isNullOrBlank()) {
            "Vedtaksbrevet er ikke signert av beslutter"
        }
        when (vedtaksbrev.saksbehandlerident) {
            IKKE_SATT_IDENT_PÅ_GAMLE_VEDTAKSBREV -> validerUlikeSignaturnavn(vedtaksbrev)
            else -> validerUlikeIdenter(vedtaksbrev)
        }
    }

    private fun validerUlikeSignaturnavn(vedtaksbrev: Vedtaksbrev) {
        if (vedtaksbrev.saksbehandlersignatur == vedtaksbrev.besluttersignatur) {
            throw ApiFeil("Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter",
                          HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerUlikeIdenter(vedtaksbrev: Vedtaksbrev) {
        if (vedtaksbrev.saksbehandlerident == vedtaksbrev.beslutterident) {
            throw ApiFeil("Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter",
                          HttpStatus.BAD_REQUEST)
        }
    }


}
