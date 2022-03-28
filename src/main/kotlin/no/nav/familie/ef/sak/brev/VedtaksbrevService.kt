package no.nav.familie.ef.sak.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.brev.domain.FRITEKST
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevRequestDto
import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.brev.dto.VedtaksbrevFritekstDto
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VedtaksbrevService(private val brevClient: BrevClient,
                         private val brevRepository: VedtaksbrevRepository,
                         private val personopplysningerService: PersonopplysningerService,
                         private val brevsignaturService: BrevsignaturService,
                         private val familieDokumentClient: FamilieDokumentClient,
                         private val featureToggleService: FeatureToggleService) {

    fun hentBeslutterbrevEllerRekonstruerSaksbehandlerBrev(behandlingId: UUID): ByteArray {
        val vedtaksbrev = brevRepository.findByIdOrThrow(behandlingId)
        return when (vedtaksbrev.beslutterPdf) {
            null -> {
                feilHvis(vedtaksbrev.saksbehandlerHtml == null) {
                    "Saksbehandlerbrev ikke funnet."
                }
                familieDokumentClient.genererPdfFraHtml(vedtaksbrev.saksbehandlerHtml)
            }
            else -> vedtaksbrev.beslutterPdf.bytes
        }
    }

    fun lagSaksbehandlerSanitybrev(saksbehandling: Saksbehandling, brevrequest: JsonNode, brevmal: String): ByteArray {
        validerRedigerbarBehandling(saksbehandling)

        val saksbehandlersignatur = brevsignaturService.lagSignaturMedEnhet(saksbehandling)

        val html = brevClient.genererHtml(brevmal = brevmal,
                                          saksbehandlerBrevrequest = brevrequest,
                                          saksbehandlersignatur = saksbehandlersignatur.navn,
                                          enhet = saksbehandlersignatur.enhet,
                                          skjulBeslutterSignatur = saksbehandlersignatur.skjulBeslutter)


        lagreEllerOppdaterSaksbehandlerVedtaksbrev(behandlingId = saksbehandling.id,
                                                   brevmal = brevmal,
                                                   saksbehandlersignatur = saksbehandlersignatur.navn,
                                                   enhet = saksbehandlersignatur.enhet,
                                                   saksbehandlerHtml = html)

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    private fun lagreEllerOppdaterSaksbehandlerVedtaksbrev(behandlingId: UUID,
                                                           brevmal: String,
                                                           saksbehandlersignatur: String,
                                                           enhet: String,
                                                           saksbehandlerHtml: String): Vedtaksbrev {
        val vedtaksbrev = Vedtaksbrev(behandlingId = behandlingId,
                                      saksbehandlerHtml = saksbehandlerHtml,
                                      brevmal = brevmal,
                                      saksbehandlersignatur = saksbehandlersignatur,
                                      enhet = enhet,
                                      saksbehandlerident = SikkerhetContext.hentSaksbehandler(true))

        return when (brevRepository.existsById(behandlingId)) {
            true -> brevRepository.update(vedtaksbrev)
            false -> brevRepository.insert(vedtaksbrev)
        }
    }

    fun forhåndsvisBeslutterBrev(saksbehandling: Saksbehandling): ByteArray {
        val vedtaksbrev = brevRepository.findByIdOrThrow(saksbehandling.id)
        val signaturMedEnhet = brevsignaturService.lagSignaturMedEnhet(saksbehandling)

        feilHvis(vedtaksbrev.saksbehandlerHtml == null) {
            "Mangler saksbehandlerbrev"
        }

        return lagBeslutterPdfMedSignatur(vedtaksbrev.saksbehandlerHtml,
                                          signaturMedEnhet).bytes
    }


    fun lagEndeligBeslutterbrev(saksbehandling: Saksbehandling): Fil {
        val vedtaksbrev = brevRepository.findByIdOrThrow(saksbehandling.id)

        validerKanLageBeslutterbrev(saksbehandling, vedtaksbrev)

        val signaturMedEnhet = brevsignaturService.lagSignaturMedEnhet(saksbehandling)
        val beslutterIdent = SikkerhetContext.hentSaksbehandler(true)
        val besluttervedtaksbrev = vedtaksbrev.copy(besluttersignatur = signaturMedEnhet.navn,
                                                    enhet = signaturMedEnhet.enhet,
                                                    beslutterident = beslutterIdent)

        validerUlikeIdenter(vedtaksbrev.saksbehandlerident, beslutterIdent)

        feilHvis(vedtaksbrev.saksbehandlerHtml == null) {
            "Mangler saksbehandlerbrev"
        }

        val beslutterPdf = lagBeslutterPdfMedSignatur(vedtaksbrev.saksbehandlerHtml, signaturMedEnhet)

        val besluttervedtaksbrevMedPdf = besluttervedtaksbrev.copy(beslutterPdf = beslutterPdf)
        brevRepository.update(besluttervedtaksbrevMedPdf)
        return Fil(bytes = beslutterPdf.bytes)
    }

    private fun validerKanLageBeslutterbrev(behandling: Saksbehandling, vedtaksbrev: Vedtaksbrev) {
        if (behandling.steg != StegType.BESLUTTE_VEDTAK || behandling.status != BehandlingStatus.FATTER_VEDTAK) {
            throw Feil("Behandling er i feil steg=${behandling.steg} status=${behandling.status}",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }

        if (featureToggleService.isEnabled("familie.ef.sak.skal-validere-beslutterpdf-er-null")){
            feilHvisIkke(vedtaksbrev.beslutterPdf == null){
                "Det finnes allerede et beslutterbrev"
            }
        }

    }

    private fun lagBeslutterPdfMedSignatur(saksbehandlerHtml: String,
                                           signaturMedEnhet: SignaturDto): Fil {
        val htmlMedBeslutterSignatur = settInnBeslutterSignaturIHtml(html = saksbehandlerHtml,
                                                                     signaturMedEnhet = signaturMedEnhet)
        return Fil(familieDokumentClient.genererPdfFraHtml(htmlMedBeslutterSignatur))
    }

    private fun settInnBeslutterSignaturIHtml(html: String, signaturMedEnhet: SignaturDto): String {

        feilHvisIkke(html.contains(BESLUTTER_SIGNATUR_PLACEHOLDER)) {
            "Brev-HTML mangler placeholder for besluttersignatur"
        }

        val beslutterSignatur = if (signaturMedEnhet.skjulBeslutter) "" else signaturMedEnhet.navn
        return html.replace(BESLUTTER_SIGNATUR_PLACEHOLDER, beslutterSignatur)
    }

    fun lagSaksbehandlerFritekstbrev(fritekstbrevDto: VedtaksbrevFritekstDto, saksbehandling: Saksbehandling): ByteArray {
        validerRedigerbarBehandling(saksbehandling)
        val navn = personopplysningerService.hentGjeldeneNavn(listOf(saksbehandling.ident)).getValue(saksbehandling.ident)
        val request = FrittståendeBrevRequestDto(overskrift = fritekstbrevDto.overskrift,
                                                 avsnitt = fritekstbrevDto.avsnitt,
                                                 personIdent = saksbehandling.ident,
                                                 navn = navn)

        val signaturMedEnhet = brevsignaturService.lagSignaturMedEnhet(saksbehandling)

        val html = brevClient.genererHtmlFritekstbrev(fritekstBrev = request,
                                                      saksbehandlerNavn = signaturMedEnhet.navn,
                                                      enhet = signaturMedEnhet.enhet)

        lagreEllerOppdaterSaksbehandlerVedtaksbrev(behandlingId = fritekstbrevDto.behandlingId,
                                                   brevmal = FRITEKST,
                                                   saksbehandlersignatur = signaturMedEnhet.navn,
                                                   enhet = signaturMedEnhet.enhet,
                                                   saksbehandlerHtml = html)

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    private fun validerRedigerbarBehandling(saksbehandling: Saksbehandling) {
        if (saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            throw Feil("Behandling er i feil steg=${saksbehandling.steg} status=${saksbehandling.status}",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerUlikeIdenter(saksbehandlerIdent: String, beslutterIdent: String) {
        if (saksbehandlerIdent == beslutterIdent) {
            throw ApiFeil("Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter",
                          HttpStatus.BAD_REQUEST)
        }
    }

    fun slettVedtaksbrev(saksbehandling: Saksbehandling) {
        brevRepository.deleteById(saksbehandling.id)
    }

    companion object {

        const val BESLUTTER_SIGNATUR_PLACEHOLDER = "BESLUTTER_SIGNATUR"
    }

}
