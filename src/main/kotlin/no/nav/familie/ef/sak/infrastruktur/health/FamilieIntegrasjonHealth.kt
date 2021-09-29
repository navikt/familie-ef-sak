package no.nav.familie.ef.sak.infrastruktur.health

import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.http.health.AbstractHealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class FamilieIntegrasjonHealth(personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient)
    : AbstractHealthIndicator(personopplysningerIntegrasjonerClient, "familie.integrasjoner")
