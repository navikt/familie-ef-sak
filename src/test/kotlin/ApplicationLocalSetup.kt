package no.nav.familie.ef.sak

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class ApplicationLocalSetup
