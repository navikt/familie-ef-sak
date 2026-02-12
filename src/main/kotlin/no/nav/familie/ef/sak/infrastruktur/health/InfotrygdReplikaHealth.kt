package no.nav.familie.ef.sak.infrastruktur.health

import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.restklient.health.AbstractHealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class InfotrygdReplikaHealth(
    client: InfotrygdReplikaClient,
) : AbstractHealthIndicator(client, "infotrygd.replika")
