package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.mockk
import no.nav.familie.ef.sak.minside.MinSideKafkaProducerService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
class KafkaMock {

    @Profile("mock-kafka")
    @Bean
    @Primary
    fun minSideKafkaProducerService(): MinSideKafkaProducerService {
        return mockk(relaxed = true)
    }
}
