package no.nav.familie.ef.sak.vilkår

import java.time.LocalDate

data class StatsborgerskapDto(val land: String,
                              val gyldigFraOgMedDato: LocalDate?,
                              val gyldigTilOgMedDato: LocalDate?)