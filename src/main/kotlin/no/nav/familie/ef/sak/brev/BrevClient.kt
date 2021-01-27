package no.nav.familie.ef.sak.brev

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.nio.charset.StandardCharsets


@Component
class BrevClient(@Value("\${FAMILIE_BREV_API_URL}")
                 private val familieBrevUri: String) {

    val restTemplate = RestTemplate(listOf(StringHttpMessageConverter(StandardCharsets.UTF_8),
                                           ByteArrayHttpMessageConverter(),
                                           MappingJackson2HttpMessageConverter(objectMapper)))

    fun genererBrev(målform: String, malnavn: String, body: String): ByteArray {
        val url = URI.create("$familieBrevUri/api/ef-brev/dokument/bokmaal/testDokument/pdf")

        val request = RequestEntity.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptCharset(Charsets.UTF_8)
                .header(NavHttpHeaders.NAV_CALL_ID.asString(), MDC.get(MDCConstants.MDC_CALL_ID))
                .body(body)

        secureLogger.info("Kaller familie brev($url) med data ${body}")
        val response = restTemplate.exchange(request, ByteArray::class.java)
        return response.body ?: error("Klarte ikke generere brev med familie-brev")
    }

    companion object {

        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}

