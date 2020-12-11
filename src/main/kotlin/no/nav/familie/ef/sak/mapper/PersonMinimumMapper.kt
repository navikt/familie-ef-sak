package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.PersonMinimumDto
import no.nav.familie.ef.sak.repository.domain.søknad.PersonMinimum

object PersonMinimumMapper {

    fun tilDto(personMinimum: PersonMinimum): PersonMinimumDto {
        return PersonMinimumDto(
                navn = personMinimum.navn,
                fødselsdato = personMinimum.fødselsdato,
                personIdent = personMinimum.fødselsnummer
        )
    }
}