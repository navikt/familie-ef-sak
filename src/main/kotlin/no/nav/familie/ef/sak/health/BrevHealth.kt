package no.nav.familie.ef.sak.health

import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.http.health.AbstractHealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class BrevHealth(client: BrevClient)
    : AbstractHealthIndicator(client, "familie.brev")
