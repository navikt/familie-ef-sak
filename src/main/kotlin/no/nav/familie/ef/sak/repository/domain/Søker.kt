package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("soker")
data class Søker(@Column("fodselsnummer") val fødselsnummer: String,
                 val navn: String)

object SøkerMapper {
    fun toDomain(søknad: SøknadOvergangsstønad): Søker {
        return Søker(søknad.personalia.verdi.fødselsnummer.verdi.verdi,
                     søknad.personalia.verdi.navn.verdi)
    }


}