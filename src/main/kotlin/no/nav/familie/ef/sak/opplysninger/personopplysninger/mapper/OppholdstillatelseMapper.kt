package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.OppholdType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.OppholdstillatelseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Opphold
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Oppholdstillatelse

object OppholdstillatelseMapper {

    fun map(opphold: List<Opphold>): List<OppholdstillatelseDto> =
            opphold.map {
                OppholdstillatelseDto(fraDato = it.oppholdFra,
                                      tilDato = it.oppholdTil,
                                      oppholdstillatelse = mapOppholdstillatelse(it.type))
            }

    private fun mapOppholdstillatelse(oppholdstillatelse: Oppholdstillatelse): OppholdType {
        return when (oppholdstillatelse) {
            Oppholdstillatelse.PERMANENT -> OppholdType.PERMANENT
            Oppholdstillatelse.MIDLERTIDIG -> OppholdType.MIDLERTIDIG
            Oppholdstillatelse.OPPLYSNING_MANGLER -> OppholdType.UKJENT
        }
    }
}