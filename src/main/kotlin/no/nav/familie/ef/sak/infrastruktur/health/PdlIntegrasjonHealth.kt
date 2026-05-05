package no.nav.familie.ef.sak.infrastruktur.health

import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.restklient.health.AbstractHealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class PdlIntegrasjonHealth(
    client: PdlClient,
) : AbstractHealthIndicator(client, "pdl.personinfo")
