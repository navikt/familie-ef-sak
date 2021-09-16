package no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad

import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate

interface IPersonMinimum {

    val fødselsnummer: String?
}

data class PersonMinimum(val navn: String,
                         @Column("fodselsnummer")
                         override val fødselsnummer: String? = null,
                         @Column("fodselsdato")
                         val fødselsdato: LocalDate? = null) : IPersonMinimum
