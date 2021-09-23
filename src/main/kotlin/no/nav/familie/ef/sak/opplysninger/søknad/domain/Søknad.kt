package no.nav.familie.ef.sak.opplysninger.søknad.domain

import no.nav.familie.ef.sak.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("grunnlag_soknad")
data class Søknad(@Id
                  val id: UUID = UUID.randomUUID(),
                  val behandlingId: UUID,
                  val type: SøknadType,
                  val soknadsskjemaId: UUID,
                  val journalpostId: String,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar(),
                  @MappedCollection(idColumn = "grunnlag_soknad_id")
                  val søker: Søker,
                  val relaterteFnr: Set<String>)


enum class SøknadType {
    OVERGANGSSTØNAD,
    BARNETILSYN,
    SKOLEPENGER
}

object SøknadMapper {

    fun toDomain(journalpostId: String,
                 søknad: ISøknadsskjema,
                 behandlingId: UUID): Søknad {
        return Søknad(soknadsskjemaId = søknad.id,
                      behandlingId = behandlingId,
                      journalpostId = journalpostId,
                      søker = Søker(søknad.fødselsnummer, søknad.navn),
                      type = søknad.type,
                      relaterteFnr = søknad.barn.map { listOf(it.fødselsnummer, it.annenForelder?.person?.fødselsnummer) }
                              .flatten()
                              .filterNotNull()
                              .toSet())
    }


}
