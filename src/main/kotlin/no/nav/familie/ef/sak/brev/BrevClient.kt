package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.blankett.BlankettPdfRequest
import no.nav.familie.ef.sak.brev.VedtaksbrevService.Companion.BESLUTTER_ENHET_PLACEHOLDER
import no.nav.familie.ef.sak.brev.VedtaksbrevService.Companion.BESLUTTER_SIGNATUR_PLACEHOLDER
import no.nav.familie.ef.sak.brev.VedtaksbrevService.Companion.BESLUTTER_VEDTAKSDATO_PLACEHOLDER
import no.nav.familie.ef.sak.brev.domain.FRITEKST
import no.nav.familie.ef.sak.brev.dto.FritekstBrevMedSignaturRequest
import no.nav.familie.ef.sak.felles.util.medContentTypeJsonUTF8
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import tools.jackson.databind.JsonNode
import java.net.URI

@Component
class BrevClient(
    @Value("\${FAMILIE_BREV_API_URL}")
    private val familieBrevUri: String,
    @Qualifier("utenAuthRestClient")
    private val restClient: RestClient,
) {
    fun genererHtml(
        brevmal: String,
        saksbehandlerBrevrequest: JsonNode,
        saksbehandlersignatur: String,
        saksbehandlerEnhet: String?,
        skjulBeslutterSignatur: Boolean,
    ): String {
        feilHvis(brevmal === FRITEKST) {
            "HTML-generering av fritekstbrev er ikke implementert"
        }

        val url = URI.create("$familieBrevUri/api/ef-brev/avansert-dokument/bokmaal/$brevmal/html")

        return restClient
            .post()
            .uri(url)
            .headers { it.addAll(HttpHeaders().medContentTypeJsonUTF8()) }
            .body(
                BrevRequestMedSignaturer(
                    brevFraSaksbehandler = saksbehandlerBrevrequest,
                    saksbehandlersignatur = saksbehandlersignatur,
                    saksbehandlerEnhet = saksbehandlerEnhet,
                    besluttersignatur = BESLUTTER_SIGNATUR_PLACEHOLDER,
                    beslutterEnhet = BESLUTTER_ENHET_PLACEHOLDER,
                    skjulBeslutterSignatur = skjulBeslutterSignatur,
                    datoPlaceholder = BESLUTTER_VEDTAKSDATO_PLACEHOLDER,
                ),
            ).retrieve()
            .body<String>()!!
    }

    fun genererBlankett(blankettPdfRequest: BlankettPdfRequest): ByteArray {
        val pdfUrl = URI.create("$familieBrevUri/blankett/pdf")
        return restClient
            .post()
            .uri(pdfUrl)
            .headers { it.addAll(HttpHeaders().medContentTypeJsonUTF8()) }
            .body(blankettPdfRequest)
            .retrieve()
            .body<ByteArray>()!!
    }

    fun genererFritekstBrev(request: FritekstBrevMedSignaturRequest): ByteArray {
        val url = URI.create("$familieBrevUri/api/fritekst-brev")
        return restClient
            .post()
            .uri(url)
            .headers { it.addAll(HttpHeaders().medContentTypeJsonUTF8()) }
            .body(request)
            .retrieve()
            .body<ByteArray>()!!
    }

    companion object {
        const val EF = "ef-brev"
    }
}

data class BrevRequestMedSignaturer(
    val brevFraSaksbehandler: JsonNode,
    val saksbehandlersignatur: String,
    val saksbehandlerEnhet: String?,
    val besluttersignatur: String?,
    val beslutterEnhet: String?,
    val skjulBeslutterSignatur: Boolean,
    val datoPlaceholder: String,
)
