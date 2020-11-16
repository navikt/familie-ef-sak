package no.nav.familie.ef.sak.integration

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.config.RestTemplateBuilderBean
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.oppdrag.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI


@Service
@Import(RestTemplateBuilderBean::class)
class OppdragClient(@Value("\${FAMILIE_OPPDRAG_API_URL}")
                    private val familieOppdragUri: URI,
                    @Qualifier("azure")
                    restOperations: RestOperations) : AbstractRestClient(restOperations, "familie.oppdrag") {

    private val postOppdragUri: URI = UriComponentsBuilder.fromUri(familieOppdragUri).pathSegment("api/oppdrag").build().toUri()

    private val getStatusUri: URI = UriComponentsBuilder.fromUri(familieOppdragUri).pathSegment("api/status").build().toUri()

    private val grensesnittavstemmingUri: URI =
            UriComponentsBuilder.fromUri(familieOppdragUri).pathSegment("api/grensesnittavstemming").build().toUri()
    private val konsistensavstemmingUri: URI =
            UriComponentsBuilder.fromUri(familieOppdragUri).pathSegment("api/konsistensavstemming").build().toUri()

    fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): String {
        return postForEntity<Ressurs<String>>(postOppdragUri, utbetalingsoppdrag).getDataOrThrow()
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatus {
        return postForEntity<Ressurs<OppdragStatus>>(getStatusUri, oppdragId).getDataOrThrow()
    }

    fun grensesnittavstemming(grensesnittavstemmingRequest: GrensesnittavstemmingRequest): String {
        return postForEntity<Ressurs<String>>(grensesnittavstemmingUri, grensesnittavstemmingRequest).getDataOrThrow()
    }

    fun konsistensavstemming(konsistensavstemmingRequest: KonsistensavstemmingRequest): String {
        return postForEntity<Ressurs<String>>(konsistensavstemmingUri, konsistensavstemmingRequest).getDataOrThrow()
    }

}
