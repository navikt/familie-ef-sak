package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Kjønn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType

object KjønnMapper {

    fun tilKjønn(kjønn: KjønnType): Kjønn = kjønn.let { Kjønn.valueOf(it.name) }
}