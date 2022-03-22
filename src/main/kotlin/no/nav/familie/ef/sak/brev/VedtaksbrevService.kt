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
import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.brev.dto.VedtaksbrevFritekstDto
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
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
                         private val featureToggleService: FeatureToggleService,
                         private val familieDokumentClient: FamilieDokumentClient) {

    fun hentBeslutterbrevEllerRekonstruerSaksbehandlerBrev(behandlingId: UUID): ByteArray {
        val vedtaksbrev = brevRepository.findByIdOrThrow(behandlingId)
        return if (vedtaksbrev.beslutterPdf != null) {
            vedtaksbrev.beslutterPdf.bytes
        } else if (vedtaksbrev.saksbehandlerHtml != null) {
            familieDokumentClient.genererPdfFraHtml(vedtaksbrev.saksbehandlerHtml)
        } else {
            // Besluttersignatur er ikke laget ennå skjulBeslutterSignatur vil ikke ha noen betydning.
            brevClient.genererBrev(vedtaksbrev.tilDto(true))
        }
    }

    fun lagSaksbehandlerSanitybrev(behandlingId: UUID, brevrequest: JsonNode, brevmal: String): ByteArray {
        val behandling = behandlingService.hentBehandling(behandlingId)
        validerRedigerbarBehandling(behandling)

        val saksbehandlersignatur = brevsignaturService.lagSignaturMedEnhet(behandling.fagsakId)

        val html = brevClient.genererHtml(brevmal = brevmal,
                                          saksbehandlerBrevrequest = brevrequest,
                                          saksbehandlersignatur = saksbehandlersignatur.navn,
                                          enhet = saksbehandlersignatur.enhet,
                                          skjulBeslutterSignatur = saksbehandlersignatur.skjulBeslutter)


        lagreEllerOppdaterSaksbehandlerVedtaksbrev(behandlingId,
                                                   "", // TODO: Dette feltet skal fjernes senere
                                                   brevmal,
                                                   saksbehandlersignatur.navn,
                                                   saksbehandlersignatur.enhet,
                                                   html)

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    private fun lagreEllerOppdaterSaksbehandlerVedtaksbrev(behandlingId: UUID,
                                                           brevrequest: String,
                                                           brevmal: String,
                                                           saksbehandlersignatur: String,
                                                           enhet: String,
                                                           saksbehandlerHtml: String? = null): Vedtaksbrev {
        val vedtaksbrev = Vedtaksbrev(behandlingId = behandlingId,
                                      saksbehandlerBrevrequest = brevrequest,
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


    fun lagBeslutterBrev(behandlingId: UUID): ByteArray {
        val behandling = behandlingService.hentBehandling(behandlingId)
        validerBehandlingKanBesluttes(behandling)

        val vedtaksbrev = brevRepository.findByIdOrThrow(behandlingId)
        val signaturMedEnhet = brevsignaturService.lagSignaturMedEnhet(behandling.fagsakId)
        val besluttervedtaksbrev = vedtaksbrev.copy(
                besluttersignatur = signaturMedEnhet.navn,
                enhet = signaturMedEnhet.enhet,
                beslutterident = SikkerhetContext.hentSaksbehandler(true)
        )

        validerBeslutterIkkeErLikSaksbehandler(besluttervedtaksbrev)

        val beslutterPdf = lagBeslutterPdfMedSignatur(besluttervedtaksbrev, signaturMedEnhet)

        val besluttervedtaksbrevMedPdf = besluttervedtaksbrev.copy(beslutterPdf = beslutterPdf)
        brevRepository.update(besluttervedtaksbrevMedPdf)
        return beslutterPdf.bytes
    }

    private fun validerBehandlingKanBesluttes(behandling: Behandling) {
        if (behandling.steg != StegType.BESLUTTE_VEDTAK || behandling.status != BehandlingStatus.FATTER_VEDTAK) {
            throw Feil("Behandling er i feil steg=${behandling.steg} status=${behandling.status}",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
    }

    private fun lagBeslutterPdfMedSignatur(besluttervedtaksbrev: Vedtaksbrev,
                                           signaturMedEnhet: SignaturDto) =
            when (besluttervedtaksbrev.saksbehandlerHtml != null) { // TODO: saksbehandlerHtml skal kanskje bli ikke-nullable.
                true -> {
                    val htmlMedBeslutterSignatur = settInnBeslutterSignaturIHtml(html = besluttervedtaksbrev.saksbehandlerHtml,
                                                                                 signaturMedEnhet = signaturMedEnhet)
                    Fil(familieDokumentClient.genererPdfFraHtml(htmlMedBeslutterSignatur))

                }
                false -> // TODO: Denne branchen kan fjernes når gamle brev er besluttet
                    Fil(brevClient.genererBrev(besluttervedtaksbrev.tilDto(signaturMedEnhet.skjulBeslutter)))
            }

    private fun settInnBeslutterSignaturIHtml(html: String, signaturMedEnhet: SignaturDto): String {

        feilHvisIkke(html.contains(BESLUTTER_SIGNATUR_PLACEHOLDER)) {
            "Brev-HTML mangler placeholder for besluttersignatur"
        }

        val beslutterSignatur =  if (signaturMedEnhet.skjulBeslutter) "" else signaturMedEnhet.navn
        return html.replace(BESLUTTER_SIGNATUR_PLACEHOLDER, beslutterSignatur)
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

        val signaturMedEnhet = brevsignaturService.lagSignaturMedEnhet(behandling.fagsakId)




        val vedtaksbrev = lagreEllerOppdaterSaksbehandlerVedtaksbrev(behandlingId = frittståendeBrevDto.behandlingId,
                                                                     brevrequest = objectMapper.writeValueAsString(request),
                                                                     brevmal = FRITEKST,
                                                                     saksbehandlersignatur = signaturMedEnhet.navn,
                                                                     enhet = signaturMedEnhet.enhet)

        return brevClient.genererBrev(vedtaksbrev = vedtaksbrev.tilDto(signaturMedEnhet.skjulBeslutter))
    }

    private fun validerRedigerbarBehandling(behandling: Behandling) {
        if (behandling.status.behandlingErLåstForVidereRedigering()) {
            throw Feil("Behandling er i feil steg=${behandling.steg} status=${behandling.status}",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
    }

    private fun validerBeslutterIkkeErLikSaksbehandler(vedtaksbrev: Vedtaksbrev) {
        brukerfeilHvis(vedtaksbrev.beslutterident.isNullOrBlank()) {
            "Vedtaksbrevet er ikke signert av beslutter"
        }

        validerUlikeIdenter(vedtaksbrev)
    }

    private fun validerUlikeIdenter(vedtaksbrev: Vedtaksbrev) {
        if (vedtaksbrev.saksbehandlerident == vedtaksbrev.beslutterident) {
            throw ApiFeil("Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter",
                          HttpStatus.BAD_REQUEST)
        }
    }

    companion object {

        const val BESLUTTER_SIGNATUR_PLACEHOLDER = "BESLUTTER_SIGNATUR"
    }

}
