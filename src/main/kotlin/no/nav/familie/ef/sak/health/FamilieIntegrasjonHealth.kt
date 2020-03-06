package no.nav.familie.ef.sak.health

import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.http.health.AbstractHealthIndicator
import org.springframework.stereotype.Component

@Component
class FamilieIntegrasjonHealth(familieIntegrasjonerClient: FamilieIntegrasjonerClient)
    : AbstractHealthIndicator(familieIntegrasjonerClient, "familie.integrasjoner")
