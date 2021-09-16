package no.nav.familie.ef.sak.infrastruktur.health

import no.nav.familie.ef.sak.vedtaksbrev.BrevClient
import no.nav.familie.http.health.AbstractHealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class BrevHealth(client: BrevClient)
    : AbstractHealthIndicator(client, "familie.brev")
