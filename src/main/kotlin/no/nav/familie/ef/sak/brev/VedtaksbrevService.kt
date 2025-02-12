package no.nav.familie.ef.sak.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class VedtaksbrevService(
    private val brevClient: BrevClient,
    private val brevRepository: VedtaksbrevRepository,
    private val brevsignaturService: BrevsignaturService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val tilordnetRessursService: TilordnetRessursService,
    private val vedtakService: VedtakService,
) {
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

    fun lagSaksbehandlerSanitybrev(
        saksbehandling: Saksbehandling,
        brevrequest: JsonNode,
        brevmal: String,
    ): ByteArray {
        validerRedigerbarBehandling(saksbehandling)
        val vedtak = vedtakService.hentVedtak(saksbehandling.id)
        val vedtakErUtenBeslutter = vedtak.utledVedtakErUtenBeslutter()
        val saksbehandlersignatur = brevsignaturService.lagSignaturMedEnhet(saksbehandling, vedtakErUtenBeslutter)

        val html =
            brevClient.genererHtml(
                brevmal = brevmal,
                saksbehandlerBrevrequest = brevrequest,
                saksbehandlersignatur = saksbehandlersignatur.navn,
                enhet = saksbehandlersignatur.enhet,
                skjulBeslutterSignatur = saksbehandlersignatur.skjulBeslutter,
            )

        lagreEllerOppdaterSaksbehandlerVedtaksbrev(
            behandlingId = saksbehandling.id,
            brevmal = brevmal,
            saksbehandlersignatur = saksbehandlersignatur.navn,
            enhet = saksbehandlersignatur.enhet,
            saksbehandlerHtml = html,
        )

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    private fun lagreEllerOppdaterSaksbehandlerVedtaksbrev(
        behandlingId: UUID,
        brevmal: String,
        saksbehandlersignatur: String,
        enhet: String,
        saksbehandlerHtml: String,
    ): Vedtaksbrev {
        val vedtaksbrev =
            Vedtaksbrev(
                behandlingId = behandlingId,
                saksbehandlerHtml = saksbehandlerHtml,
                brevmal = brevmal,
                saksbehandlersignatur = saksbehandlersignatur,
                enhet = enhet,
                saksbehandlerident = SikkerhetContext.hentSaksbehandler(),
                opprettetTid = SporbarUtils.now(),
            )

        return when (brevRepository.existsById(behandlingId)) {
            true -> brevRepository.update(vedtaksbrev)
            false -> brevRepository.insert(vedtaksbrev)
        }
    }

    fun forhåndsvisBeslutterBrev(saksbehandling: Saksbehandling): ByteArray {
        val vedtaksbrev = brevRepository.findByIdOrThrow(saksbehandling.id)
        val vedtak = vedtakService.hentVedtak(saksbehandling.id)
        val vedtakErUtenBeslutter = vedtak.utledVedtakErUtenBeslutter()
        val signaturMedEnhet = brevsignaturService.lagSignaturMedEnhet(saksbehandling, vedtakErUtenBeslutter)

        feilHvis(vedtaksbrev.saksbehandlerHtml == null) {
            "Mangler saksbehandlerbrev"
        }

        return lagBeslutterPdfMedSignatur(
            vedtaksbrev.saksbehandlerHtml,
            signaturMedEnhet,
        ).bytes
    }

    fun lagEndeligBeslutterbrev(
        saksbehandling: Saksbehandling,
        vedtakErUtenBeslutter: VedtakErUtenBeslutter,
    ): Fil {
        val vedtaksbrev = brevRepository.findByIdOrThrow(saksbehandling.id)
        val saksbehandlerHtml = hentSaksbehandlerHtml(vedtaksbrev, saksbehandling)
        val beslutterIdent = SikkerhetContext.hentSaksbehandler()
        validerKanLageBeslutterbrev(saksbehandling, vedtaksbrev, beslutterIdent, vedtakErUtenBeslutter)
        val signaturMedEnhet = brevsignaturService.lagSignaturMedEnhet(saksbehandling, vedtakErUtenBeslutter)
        val beslutterPdf = lagBeslutterPdfMedSignatur(saksbehandlerHtml, signaturMedEnhet)
        val besluttervedtaksbrev =
            vedtaksbrev.copy(
                besluttersignatur = signaturMedEnhet.navn,
                enhet = signaturMedEnhet.enhet,
                beslutterident = beslutterIdent,
                beslutterPdf = beslutterPdf,
                besluttetTid = LocalDateTime.now(),
            )
        brevRepository.update(besluttervedtaksbrev)
        return Fil(bytes = beslutterPdf.bytes)
    }

    private fun hentSaksbehandlerHtml(
        vedtaksbrev: Vedtaksbrev,
        saksbehandling: Saksbehandling,
    ): String {
        feilHvis(vedtaksbrev.saksbehandlerHtml == null) {
            "Mangler saksbehandlerbrev for behandling: ${saksbehandling.id}"
        }
        feilHvis(vedtaksbrev.saksbehandlerHtml.isEmpty()) {
            "Mangler innhold i saksbehandlerbrev for behandling: ${saksbehandling.id}"
        }
        return vedtaksbrev.saksbehandlerHtml
    }

    private fun validerKanLageBeslutterbrev(
        behandling: Saksbehandling,
        vedtaksbrev: Vedtaksbrev,
        beslutterIdent: String,
        vedtakErUtenBeslutter: VedtakErUtenBeslutter,
    ) {
        if (behandling.steg != StegType.BESLUTTE_VEDTAK || behandling.status != BehandlingStatus.FATTER_VEDTAK) {
            throw Feil(
                "Behandling er i feil steg=${behandling.steg} status=${behandling.status}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        feilHvisIkke(vedtaksbrev.beslutterPdf == null) {
            "Det finnes allerede et beslutterbrev"
        }
        if (!vedtakErUtenBeslutter.value) {
            validerUlikeIdenter(vedtaksbrev.saksbehandlerident, beslutterIdent)
        }
    }

    private fun lagBeslutterPdfMedSignatur(
        saksbehandlerHtml: String,
        signaturMedEnhet: SignaturDto,
    ): Fil {
        val htmlMedBeslutterSignatur =
            settInnBeslutterSignaturIHtml(
                html = saksbehandlerHtml,
                signaturMedEnhet = signaturMedEnhet,
            )
        return Fil(familieDokumentClient.genererPdfFraHtml(htmlMedBeslutterSignatur))
    }

    private fun settInnBeslutterSignaturIHtml(
        html: String,
        signaturMedEnhet: SignaturDto,
    ): String {
        feilHvis(!signaturMedEnhet.skjulBeslutter && !html.contains(BESLUTTER_SIGNATUR_PLACEHOLDER)) {
            "Brev-HTML mangler placeholder for besluttersignatur"
        }

        val beslutterSignatur = if (signaturMedEnhet.skjulBeslutter) "" else signaturMedEnhet.navn
        return html
            .replace(BESLUTTER_SIGNATUR_PLACEHOLDER, beslutterSignatur)
            .replace(BESLUTTER_VEDTAKSDATO_PLACEHOLDER, LocalDate.now().norskFormat())
    }

    private fun validerRedigerbarBehandling(saksbehandling: Saksbehandling) {
        if (saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            throw ApiFeil(
                "Behandling er i feil steg=${saksbehandling.steg} status=${saksbehandling.status}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        brukerfeilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(saksbehandling.id)) {
            "Behandlingen har en ny eier"
        }
    }

    private fun validerUlikeIdenter(
        saksbehandlerIdent: String,
        beslutterIdent: String,
    ) {
        if (saksbehandlerIdent == beslutterIdent) {
            throw ApiFeil(
                "Beslutter kan ikke behandle en behandling som den selv har sendt til beslutter",
                HttpStatus.BAD_REQUEST,
            )
        }
    }

    fun slettVedtaksbrev(saksbehandling: Saksbehandling) {
        brevRepository.deleteById(saksbehandling.id)
    }

    companion object {
        const val BESLUTTER_SIGNATUR_PLACEHOLDER = "BESLUTTER_SIGNATUR"
        const val BESLUTTER_VEDTAKSDATO_PLACEHOLDER = "BESLUTTER_VEDTAKSDATO"
    }
}
