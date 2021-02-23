package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate

data class SivilstandsplanerDto(val harPlaner: Boolean?,
                                val fraDato: LocalDate?,
                                val vordendeSamboerEktefelle: PersonMinimumDto?
)