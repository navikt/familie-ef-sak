package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.Kjønn
import no.nav.familie.ef.sak.integration.dto.pdl.KjønnType

object KjønnMapper {

    fun tilKjønn(kjønn: KjønnType): Kjønn =  kjønn.let { Kjønn.valueOf(it.name) }
}