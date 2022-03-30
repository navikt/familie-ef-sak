package no.nav.familie.ef.sak.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.brev.VedtaksbrevService.Companion.BESLUTTER_SIGNATUR_PLACEHOLDER
import no.nav.familie.ef.sak.brev.domain.FRITEKST
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevRequestDto
import no.nav.familie.ef.sak.felles.util.medContentTypeJsonUTF8
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI


@Component
class BrevClient(@Value("\${FAMILIE_BREV_API_URL}")
                 private val familieBrevUri: String,
                 @Qualifier("utenAuth")
                 private val restOperations: RestOperations) : AbstractPingableRestClient(restOperations, "familie.brev") {

    override val pingUri: URI = URI.create("$familieBrevUri/api/status")

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

    fun genererBrev(fritekstBrev: FrittståendeBrevRequestDto,
                    saksbehandlerNavn: String,
                    enhet: String = "NAV Arbeid og ytelser"): ByteArray {
        val url = URI.create("$familieBrevUri/api/fritekst-brev")
        return postForEntity(url,
                             FritekstBrevRequestMedSignatur(fritekstBrev,
                                                            saksbehandlerNavn,
                                                            null,
                                                            enhet),
                             HttpHeaders().medContentTypeJsonUTF8())
    }

    fun genererHtml(brevmal: String,
                    saksbehandlerBrevrequest: JsonNode,
                    saksbehandlersignatur: String,
                    enhet: String?,
                    skjulBeslutterSignatur: Boolean): String {

        feilHvis(brevmal === FRITEKST) {
            "HTML-generering av fritekstbrev er ikke implementert"
        }

        val url = URI.create("$familieBrevUri/api/ef-brev/avansert-dokument/bokmaal/${brevmal}/html")

        return postForEntity(url,
                             BrevRequestMedSignaturer(brevFraSaksbehandler = saksbehandlerBrevrequest,
                                                      saksbehandlersignatur = saksbehandlersignatur,
                                                      besluttersignatur = BESLUTTER_SIGNATUR_PLACEHOLDER,
                                                      enhet = enhet,
                                                      skjulBeslutterSignatur = skjulBeslutterSignatur
                             ),
                             HttpHeaders().medContentTypeJsonUTF8()
        )
    }

    fun genererHtmlFritekstbrev(fritekstBrev: FrittståendeBrevRequestDto, saksbehandlerNavn: String, enhet: String): String {
        val url = URI.create("$familieBrevUri/api/fritekst-brev/html")
        return postForEntity(url,
                             FritekstBrevRequestMedSignatur(fritekstBrev,
                                                            saksbehandlerNavn,
                                                            BESLUTTER_SIGNATUR_PLACEHOLDER,
                                                            enhet),
                             HttpHeaders().medContentTypeJsonUTF8())
    }

    companion object {

        const val ef = "ef-brev"
        const val test = "testdata"
    }
}

data class BrevRequestMedSignaturer(val brevFraSaksbehandler: JsonNode,
                                    val saksbehandlersignatur: String,
                                    val besluttersignatur: String?,
                                    val enhet: String?,
                                    val skjulBeslutterSignatur: Boolean)

data class FritekstBrevRequestMedSignatur(val brevFraSaksbehandler: FrittståendeBrevRequestDto,
                                          val saksbehandlersignatur: String,
                                          val besluttersignatur: String?,
                                          val enhet: String)
