package no.nav.familie.ef.sak.health

import no.nav.familie.ef.sak.integration.InfotrygdReplikaClient
import no.nav.familie.http.health.AbstractHealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class InfotrygdReplikaHealth(client: InfotrygdReplikaClient)
    : AbstractHealthIndicator(client, "infotrygd.replika")
