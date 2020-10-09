package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.LocalDate

data class Sivilstandsplaner(val harPlaner: Søknadsfelt<Boolean>? = null,
                             val fraDato: Søknadsfelt<LocalDate>? = null,
                             val vordendeSamboerEktefelle: Søknadsfelt<PersonMinimum>? = null)
