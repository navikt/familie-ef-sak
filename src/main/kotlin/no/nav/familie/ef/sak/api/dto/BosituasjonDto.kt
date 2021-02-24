package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate


data class BosituasjonDto(val delerDuBolig: String?,
                          val samboer: PersonMinimumDto?,
                          val sammenflyttingsdato: LocalDate?,
                          val datoFlyttetFraHverandre: LocalDate?,
                          val tidligereSamboerFortsattRegistrertPåAdresse: DokumentasjonDto?)