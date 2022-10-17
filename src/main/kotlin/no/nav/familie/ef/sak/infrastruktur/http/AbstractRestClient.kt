package no.nav.familie.ef.sak.infrastruktur.http

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.webflux.client.AbstractWebClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestOperations
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

class AbstractRestClient2(operations: RestOperations, metricsPrefix: String): AbstractRestClient(operations, metricsPrefix) {
    protected inline fun <reified T: Any> getForEntity2(uri: URI, httpHeaders: HttpHeaders? = null): T {
        return getForEntity(uri, httpHeaders)
    }

    inline fun <reified T : Any> postForEntity2(uri: URI, payload: Any, httpHeaders: HttpHeaders? = null): T {
        return super.postForEntity(uri, payload, httpHeaders)
    }

    inline fun <reified T : Any> putForEntity2(uri: URI, payload: Any, httpHeaders: HttpHeaders? = null): T {
        return super.putForEntity(uri, payload, httpHeaders)
    }

    inline fun <reified T : Any> patchForEntity2(uri: URI, payload: Any, httpHeaders: HttpHeaders? = null): T {
        return super.patchForEntity(uri, payload, httpHeaders)
    }

    inline fun <reified T : Any> deleteForEntity2(uri: URI, payload: Any? = null, httpHeaders: HttpHeaders? = null): T {
        return super.deleteForEntity(uri, payload, httpHeaders)
    }

}

abstract class AbstractRestWebClient(
    val operations: RestOperations,
    val webClient: WebClient,
    metricsPrefix: String
) {
    val rest: AbstractRestClient
    val web: AbstractWebClient
    init {
        rest = AbstractRestClient2(operations, metricsPrefix)
        web = object : AbstractWebClient(webClient, metricsPrefix) {
            inline fun <reified T : Any> getForEntity2(uri: URI, httpHeaders: HttpHeaders? = null): T {
                return super.getForEntity<T>(uri, httpHeaders)
            }

            inline fun <reified T : Any> postForEntity2(uri: URI, payload: Any, httpHeaders: HttpHeaders? = null): T {
                return super.postForEntity(uri, payload, httpHeaders)
            }

            inline fun <reified T : Any> putForEntity2(uri: URI, payload: Any, httpHeaders: HttpHeaders? = null): T {
                return super.putForEntity(uri, payload, httpHeaders)
            }

            inline fun <reified T : Any> patchForEntity2(uri: URI, payload: Any, httpHeaders: HttpHeaders? = null): T {
                return super.patchForEntity(uri, payload, httpHeaders)
            }

            inline fun <reified T : Any> deleteForEntity2(uri: URI, payload: Any? = null, httpHeaders: HttpHeaders? = null): T {
                return super.deleteForEntity(uri, httpHeaders)
            }
        }
    }

    @Autowired
    private lateinit var featureToggleService: FeatureToggleService

    protected inline fun <reified T : Any> getForEntity(uri: URI, httpHeaders: HttpHeaders? = null): T {
        if(featureToggleService.isEnabled(Toggle.WEBCLIENT)) {
            return web
        } else {
            rest.
        }
    }

    protected inline fun <reified T : Any> postForEntity(uri: URI, payload: Any, httpHeaders: HttpHeaders? = null): T {
    }

    protected inline fun <reified T : Any> putForEntity(uri: URI, payload: Any, httpHeaders: HttpHeaders? = null): T {
    }

    protected inline fun <reified T : Any> patchForEntity(uri: URI, payload: Any, httpHeaders: HttpHeaders? = null): T {
    }

    protected inline fun <reified T : Any> deleteForEntity(uri: URI, payload: Any? = null, httpHeaders: HttpHeaders? = null): T {
    }
}