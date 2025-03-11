package no.nav.familie.ef.sak.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.blankett.BlankettPdfRequest
import no.nav.familie.ef.sak.brev.VedtaksbrevService.Companion.BESLUTTER_SIGNATUR_PLACEHOLDER
import no.nav.familie.ef.sak.brev.VedtaksbrevService.Companion.BESLUTTER_VEDTAKSDATO_PLACEHOLDER
import no.nav.familie.ef.sak.brev.domain.FRITEKST
import no.nav.familie.ef.sak.brev.dto.FritekstBrevRequestMedSignatur
import no.nav.familie.ef.sak.felles.util.medContentTypeJsonUTF8
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.næringsinntektskontroll.NæringsinntektIngenEndringPdfRequest
import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class BrevClient(
    @Value("\${FAMILIE_BREV_API_URL}")
    private val familieBrevUri: String,
    @Qualifier("utenAuth")
    private val restOperations: RestOperations,
) : AbstractPingableRestClient(restOperations, "familie.brev") {
    override val pingUri: URI = URI.create("$familieBrevUri/api/status")

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun genererHtml(
        brevmal: String,
        saksbehandlerBrevrequest: JsonNode,
        saksbehandlersignatur: String,
        enhet: String?,
        skjulBeslutterSignatur: Boolean,
    ): String {
        feilHvis(brevmal === FRITEKST) {
            "HTML-generering av fritekstbrev er ikke implementert"
        }

        val url = URI.create("$familieBrevUri/api/ef-brev/avansert-dokument/bokmaal/$brevmal/html")

        return postForEntity(
            url,
            BrevRequestMedSignaturer(
                brevFraSaksbehandler = saksbehandlerBrevrequest,
                saksbehandlersignatur = saksbehandlersignatur,
                besluttersignatur = BESLUTTER_SIGNATUR_PLACEHOLDER,
                enhet = enhet,
                skjulBeslutterSignatur = skjulBeslutterSignatur,
                datoPlaceholder = BESLUTTER_VEDTAKSDATO_PLACEHOLDER,
            ),
            HttpHeaders().medContentTypeJsonUTF8(),
        )
    }

    fun genererBlankett(blankettPdfRequest: BlankettPdfRequest): ByteArray {
        val pdfUrl = URI.create("$familieBrevUri/blankett/pdf")
        return postForEntity(pdfUrl, blankettPdfRequest, HttpHeaders().medContentTypeJsonUTF8())
    }

    fun genererNæringsinntektUtenEndringNotat(næringsinntektIngenEndringPdfRequest: NæringsinntektIngenEndringPdfRequest): ByteArray {
        val url = URI.create("$familieBrevUri/naeringsinntekt-kontroll/pdf")
        return postForEntity(url, næringsinntektIngenEndringPdfRequest, HttpHeaders().medContentTypeJsonUTF8())
    }

    fun genererFritekstBrev(request: FritekstBrevRequestMedSignatur): ByteArray {
        val url = URI.create("$familieBrevUri/api/fritekst-brev")
        return postForEntity(url, request, HttpHeaders().medContentTypeJsonUTF8())
    }

    companion object {
        const val EF = "ef-brev"
    }
}

data class BrevRequestMedSignaturer(
    val brevFraSaksbehandler: JsonNode,
    val saksbehandlersignatur: String,
    val besluttersignatur: String?,
    val enhet: String?,
    val skjulBeslutterSignatur: Boolean,
    val datoPlaceholder: String,
)
