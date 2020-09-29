package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.OppholdType
import no.nav.familie.ef.sak.api.dto.OppholdstillatelseDto
import no.nav.familie.ef.sak.integration.dto.pdl.Opphold
import no.nav.familie.ef.sak.integration.dto.pdl.Oppholdstillatelse

object OppholdstillatelseMapper {

    fun map(opphold: List<Opphold>): List<OppholdstillatelseDto> =
            opphold.map {
                OppholdstillatelseDto(fraDato = it.oppholdFra,
                                      tilDato = it.oppholdTil,
                                      oppholdstillatelse = mapOppholdstillatelse(it.type))
            }

    fun mapOppholdstillatelse(oppholdstillatelse: Oppholdstillatelse): OppholdType {
        return when (oppholdstillatelse) {
            Oppholdstillatelse.PERMANENT -> OppholdType.PERMANENT
            Oppholdstillatelse.MIDLERTIDIG -> OppholdType.MIDLERTIDIG
            Oppholdstillatelse.OPPLYSNING_MANGLER -> OppholdType.UKJENT
        }
    }
}