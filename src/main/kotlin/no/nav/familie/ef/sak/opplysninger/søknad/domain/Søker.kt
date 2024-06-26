package no.nav.familie.ef.sak.opplysninger.søknad.domain

import no.nav.familie.kontrakter.ef.søknad.Personalia
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("soknad_soker")
data class Søker(
    @Column("fodselsnummer")
    val fødselsnummer: String,
    val navn: String,
)

object SøkerMapper {
    fun toDomain(personalia: Søknadsfelt<Personalia>): Søker =
        Søker(
            personalia.verdi.fødselsnummer.verdi.verdi,
            personalia.verdi.navn.verdi,
        )
}
