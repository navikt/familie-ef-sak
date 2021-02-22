package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.PersonMinimumDto
import no.nav.familie.ef.sak.api.dto.SivilstandsplanerDto
import no.nav.familie.ef.sak.repository.domain.søknad.Sivilstandsplaner

object SivilstandsplanerMapper {

    fun tilDto(sivilstandsplaner: Sivilstandsplaner): SivilstandsplanerDto {
        val samboerDto = sivilstandsplaner.vordendeSamboerEktefelle?.let { PersonMinimumDto(it.navn, it.fødselsdato, it.fødselsnummer) }

        return SivilstandsplanerDto(sivilstandsplaner.harPlaner,
                                    sivilstandsplaner.fraDato,
                                    samboerDto)
    }
}