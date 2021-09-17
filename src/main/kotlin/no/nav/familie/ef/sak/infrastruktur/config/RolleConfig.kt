package no.nav.familie.ef.sak.infrastruktur.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class RolleConfig(@Value("\${rolle.beslutter}")
                  val beslutterRolle: String,
                  @Value("\${rolle.saksbehandler}")
                  val saksbehandlerRolle: String,
                  @Value("\${rolle.veileder}")
                  val veilederRolle: String)