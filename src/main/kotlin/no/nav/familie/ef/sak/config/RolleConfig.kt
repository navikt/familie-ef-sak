package no.nav.familie.ef.sak.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class RolleConfig(
        @Value("\${BESLUTTER_ROLLE}")
        val BESLUTTER_ROLLE: String,

        @Value("\${SAKSBEHANDLER_ROLLE}")
        val SAKSBEHANDLER_ROLLE: String,

        @Value("\${VEILEDER_ROLLE}")
        val VEILEDER_ROLLE: String,
)