package no.nav.familie.ef.sak.repository.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("grunnlag_soknad")
data class Søknad(@Id
                  val id: UUID = UUID.randomUUID(),
                  val behandlingId: UUID,
                  val type: SøknadType,
                  @Column("soknad")
                  val søknad: ByteArray,
                  val saksnummerInfotrygd: String,
                  val journalpostId: String,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar(),
                  @MappedCollection(idColumn = "grunnlag_soknad_id")
                  val søker: Søker,
                  @MappedCollection(idColumn = "grunnlag_soknad_id")
                  val barn: Set<Barn>)

data class SøknadWrapper<T>(val soknad: Søknad, val søknad: T)

enum class SøknadType {
    OVERGANGSSTØNAD,
    BARNETILSYN,
    SKOLEPENGER
}

object SøknadMapper {

    fun toDomain(saksnummer: String,
                 journalpostId: String,
                 søknad: SøknadOvergangsstønad,
                 behandlingId: UUID): Søknad {
        return Søknad(søknad = objectMapper.writeValueAsBytes(søknad),
                      behandlingId = behandlingId,
                      saksnummerInfotrygd = saksnummer,
                      journalpostId = journalpostId,
                      søker = SøkerMapper.toDomain(søknad.personalia),
                      barn = BarnMapper.toDomain(søknad.barn),
                      type = SøknadType.OVERGANGSSTØNAD)
    }

    fun toDomain(saksnummer: String,
                 journalpostId: String,
                 søknad: SøknadBarnetilsyn,
                 behandlingId: UUID): Søknad {
        return Søknad(søknad = objectMapper.writeValueAsBytes(søknad),
                      behandlingId = behandlingId,
                      saksnummerInfotrygd = saksnummer,
                      journalpostId = journalpostId,
                      søker = SøkerMapper.toDomain(søknad.personalia),
                      barn = BarnMapper.toDomain(søknad.barn),
                      type = SøknadType.BARNETILSYN)
    }

    fun pakkOppOvergangsstønad(søknad: Søknad): SøknadWrapper<SøknadOvergangsstønad> {
        return pakkOpp(søknad, SøknadType.OVERGANGSSTØNAD)
    }

    fun pakkOppBarnetisyn(søknad: Søknad): SøknadWrapper<SøknadBarnetilsyn> {
        return pakkOpp(søknad, SøknadType.BARNETILSYN)
    }

    private inline fun <reified T> pakkOpp(søknad: Søknad,
                                           søknadType: SøknadType): SøknadWrapper<T> {
        if (søknad.type != søknadType) error("Feil type søknad ${søknad.id} ${søknad.type}")
        return SøknadWrapper(søknad, objectMapper.readValue(søknad.søknad))
    }

}
