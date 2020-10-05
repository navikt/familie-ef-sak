package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate

data class StatsborgerskapDto(val land: String,
                              val gyldigFraOgMedDato: LocalDate?,
                              val gyldigTilOgMedDato: LocalDate?)