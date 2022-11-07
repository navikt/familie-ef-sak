package no.nav.familie.ef.sak.infrastruktur.http

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.webflux.client.AbstractWebClient
import no.nav.familie.webflux.client.Pingable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestOperations
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

abstract class AbstractPingableRestWebClient(
    operations: RestOperations,
    webClient: WebClient,
    metricsPrefix: String,
    featureToggleService: FeatureToggleService
) : AbstractRestWebClient(operations, webClient, metricsPrefix, featureToggleService), Pingable {

    abstract val pingUri: URI

    override fun ping() {
        super.getForEntity<String>(pingUri, null)
    }
}

abstract class AbstractRestWebClient(
    val operations: RestOperations,
    val webClient: WebClient,
    metricsPrefix: String,
    val featureToggleService: FeatureToggleService
) {
    protected val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    val rest: AbstractRestClient = object : AbstractRestClient(operations, metricsPrefix) {}
    val web: AbstractWebClient = object : AbstractWebClient(webClient, metricsPrefix) {}

    inline fun <reified T : Any> getForEntity(uri: URI, httpHeaders: HttpHeaders? = null): T {
        return if (featureToggleService.isEnabled(Toggle.WEBCLIENT)) {
            web.getForEntity(uri, httpHeaders)
        } else {
            rest.getForEntity(uri, httpHeaders)
        }
    }

    inline fun <reified T : Any> postForEntity(uri: URI, payload: Any, httpHeaders: HttpHeaders? = null): T {
        return if (featureToggleService.isEnabled(Toggle.WEBCLIENT)) {
            web.postForEntity(uri, payload, httpHeaders)
        } else {
            rest.postForEntity(uri, payload, httpHeaders)
        }
    }

    inline fun <reified T : Any> putForEntity(uri: URI, payload: Any, httpHeaders: HttpHeaders? = null): T {
        return if (featureToggleService.isEnabled(Toggle.WEBCLIENT)) {
            web.putForEntity(uri, payload, httpHeaders)
        } else {
            rest.putForEntity(uri, payload, httpHeaders)
        }
    }

    inline fun <reified T : Any> patchForEntity(uri: URI, payload: Any, httpHeaders: HttpHeaders? = null): T {
        return if (featureToggleService.isEnabled(Toggle.WEBCLIENT)) {
            web.patchForEntity(uri, payload, httpHeaders)
        } else {
            rest.patchForEntity(uri, payload, httpHeaders)
        }
    }

    inline fun <reified T : Any> deleteForEntity(uri: URI, httpHeaders: HttpHeaders? = null): T {
        return if (featureToggleService.isEnabled(Toggle.WEBCLIENT)) {
            web.deleteForEntity(uri, httpHeaders)
        } else {
            rest.deleteForEntity(uri, httpHeaders)
        }
    }
}
