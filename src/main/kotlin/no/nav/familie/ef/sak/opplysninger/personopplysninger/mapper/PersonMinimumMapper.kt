package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonMinimumDto
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.PersonMinimum

object PersonMinimumMapper {

    fun tilDto(personMinimum: PersonMinimum): PersonMinimumDto {
        return PersonMinimumDto(
                navn = personMinimum.navn,
                fødselsdato = personMinimum.fødselsdato,
                personIdent = personMinimum.fødselsnummer
        )
    }
}