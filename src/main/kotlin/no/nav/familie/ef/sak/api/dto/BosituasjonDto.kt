package no.nav.familie.ef.sak.api.dto

import no.nav.familie.kontrakter.ef.søknad.EnumTekstverdiMedSvarId
import java.time.LocalDate


data class BosituasjonDto(val delerDuBolig: EnumTekstverdiMedSvarId,
                          val samboer: PersonMinimumDto?,
                          val sammenflyttingsdato: LocalDate?,
                          val datoFlyttetFraHverandre: LocalDate?,
                          val tidligereSamboerFortsattRegistrertPåAdresse: DokumentasjonDto?)