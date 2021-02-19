package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.Kjønn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker

object KjønnMapper {

    fun tilKjønn(søker: PdlSøker): Kjønn =  søker.kjønn.single().kjønn.let { Kjønn.valueOf(it.name) }
}