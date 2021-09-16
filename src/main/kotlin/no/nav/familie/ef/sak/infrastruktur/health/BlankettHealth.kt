package no.nav.familie.ef.sak.infrastruktur.health

import no.nav.familie.ef.sak.blankett.BlankettClient
import no.nav.familie.http.health.AbstractHealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class BlankettHealth(client: BlankettClient)
    : AbstractHealthIndicator(client, "familie.blankett")
