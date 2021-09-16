package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonMinimumDto
import java.time.LocalDate

data class SivilstandsplanerDto(val harPlaner: Boolean?,
                                val fraDato: LocalDate?,
                                val vordendeSamboerEktefelle: PersonMinimumDto?
)