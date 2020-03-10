package no.nav.familie.ef.sak.no.nav.familie.ef.sak.config

import no.nav.familie.http.config.INaisProxyCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
@Primary
class NaisNoProxyCustomizer: INaisProxyCustomizer {

    override fun customize(restTemplate: RestTemplate?) {
        val her = 15

    }
}
