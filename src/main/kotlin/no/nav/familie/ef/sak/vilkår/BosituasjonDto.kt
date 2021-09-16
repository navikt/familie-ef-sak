package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonMinimumDto
import java.time.LocalDate


data class BosituasjonDto(val delerDuBolig: String?,
                          val samboer: PersonMinimumDto?,
                          val sammenflyttingsdato: LocalDate?,
                          val datoFlyttetFraHverandre: LocalDate?,
                          val tidligereSamboerFortsattRegistrertPåAdresse: DokumentasjonDto?)