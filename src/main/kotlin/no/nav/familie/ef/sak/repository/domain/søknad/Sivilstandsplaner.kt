package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.LocalDate

data class Sivilstandsplaner(val harPlaner: Boolean? = null,
                             val fraDato: LocalDate? = null,
                             val vordendeSamboerEktefelle: PersonMinimum? = null)
