package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
    fun minSideKafkaProducerService(): MinSideKafkaProducerService = mockk(relaxed = true)

    val payloadSlot = slot<String>()

    @Bean
    fun kafkaProducerPayloadSlot(): CapturingSlot<String> = payloadSlot
}
