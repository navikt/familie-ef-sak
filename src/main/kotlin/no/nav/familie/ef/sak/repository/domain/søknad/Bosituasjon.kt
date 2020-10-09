package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.LocalDate

data class Bosituasjon(val delerDuBolig: String,
                       val samboerdetaljer: PersonMinimum? = null,
                       val sammenflyttingsdato: LocalDate? = null,
                       val datoFlyttetFraHverandre: LocalDate? = null,
                       val tidligereSamboerFortsattRegistrertPåAdresse: Dokumentasjon? = null)
