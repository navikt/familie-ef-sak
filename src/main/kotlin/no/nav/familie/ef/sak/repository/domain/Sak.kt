package no.nav.familie.ef.sak.repository.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("sak")
data class Sak(@Id
               val id: UUID = UUID.randomUUID(),
               val type: SøknadType,
               @Column("soknad")
               val søknad: ByteArray,
               val saksnummer: String,
               @Column("journalpost_id")
               val journalpostId: String,
               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
               val sporbar: Sporbar = Sporbar(),
               val søker: Søker,
               val barn: Set<Barn>)

data class SakWrapper<T>(val sak: Sak, val søknad: T)

enum class SøknadType {
    OVERGANGSSTØNAD,
    BARNETILSYN
}

object SakMapper {

    fun toDomain(saksnummer: String,
                 journalpostId: String,
                 søknad: SøknadOvergangsstønad): Sak {
        return Sak(søknad = objectMapper.writeValueAsBytes(søknad),
                   saksnummer = saksnummer,
                   journalpostId = journalpostId,
                   søker = SøkerMapper.toDomain(søknad.personalia),
                   barn = BarnMapper.toDomain(søknad.barn),
                   type = SøknadType.OVERGANGSSTØNAD)
    }

    fun toDomain(saksnummer: String,
                 journalpostId: String,
                 søknad: SøknadBarnetilsyn): Sak {
        return Sak(søknad = objectMapper.writeValueAsBytes(søknad),
                   saksnummer = saksnummer,
                   journalpostId = journalpostId,
                   søker = SøkerMapper.toDomain(søknad.personalia),
                   barn = BarnMapper.toDomain(søknad.barn),
                   type = SøknadType.BARNETILSYN)
    }

    fun pakkOppOvergangsstønad(sak: Sak): SakWrapper<SøknadOvergangsstønad> {
        return pakkOpp(sak, SøknadType.OVERGANGSSTØNAD)
    }

    fun pakkOppBarnetisyn(sak: Sak): SakWrapper<SøknadBarnetilsyn> {
        return pakkOpp(sak, SøknadType.BARNETILSYN)
    }

    private inline fun <reified T> pakkOpp(sak: Sak,
                                           søknadType: SøknadType): SakWrapper<T> {
        if (sak.type != søknadType) error("Feil type søknad ${sak.id} ${sak.type}")
        return SakWrapper(sak, objectMapper.readValue(sak.søknad))
    }

}
