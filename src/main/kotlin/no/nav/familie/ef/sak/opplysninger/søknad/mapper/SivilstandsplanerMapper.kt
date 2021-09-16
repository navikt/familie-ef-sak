package no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonMinimumDto
import no.nav.familie.ef.sak.vilkår.SivilstandsplanerDto
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.Sivilstandsplaner

object SivilstandsplanerMapper {

    fun tilDto(sivilstandsplaner: Sivilstandsplaner?): SivilstandsplanerDto {
        val samboerDto = sivilstandsplaner?.vordendeSamboerEktefelle?.let { PersonMinimumDto(it.navn, it.fødselsdato, it.fødselsnummer) }

        return SivilstandsplanerDto(sivilstandsplaner?.harPlaner,
                                    sivilstandsplaner?.fraDato,
                                    samboerDto)
    }
}

