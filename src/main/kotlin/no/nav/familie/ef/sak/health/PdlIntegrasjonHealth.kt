package no.nav.familie.ef.sak.health

import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.http.health.AbstractHealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class PdlIntegrasjonHealth(client: PdlClient)
    : AbstractHealthIndicator(client, "pdl.personinfo")
