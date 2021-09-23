package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate

data class Sivilstandsplaner(val harPlaner: Boolean? = null,
                             val fraDato: LocalDate? = null,
                             @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "vordende_samboer_")
                             val vordendeSamboerEktefelle: PersonMinimum? = null)
