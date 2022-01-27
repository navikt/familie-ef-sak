package no.nav.familie.ef.sak.no.nav.familie.ef.sak.infrastruktur.config

import io.mockk.mockk
import no.nav.familie.ef.sak.AuditLogger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile


@Configuration
@Profile("local", "integrasjonstest")
class AuditLoggerMock {

    @Bean
    @Primary
    fun auditLogger(): AuditLogger = mockk(relaxed = true)
}