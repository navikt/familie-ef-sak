package no.nav.familie.ef.sak.infrastruktur.health

import no.nav.familie.ef.sak.felles.integration.FamilieIntegrasjonerClient
import no.nav.familie.http.health.AbstractHealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class FamilieIntegrasjonHealth(familieIntegrasjonerClient: FamilieIntegrasjonerClient)
    : AbstractHealthIndicator(familieIntegrasjonerClient, "familie.integrasjoner")
