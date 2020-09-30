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

@Table("gr_soknad")
data class Søknad(@Id
                  val id: UUID = UUID.randomUUID(),
                  @Column("behandling_id")
                  val behandlingId: UUID,
                  val type: SøknadType,
                  @Column("soknad")
                  val søknad: ByteArray,
                  @Column("saksnummer_infotrygd")
                  val saksnummerInfotrygd: String,
                  @Column("journalpost_id")
                  val journalpostId: String,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar(),
                  val søker: Søker,
                  val barn: Set<Barn>)

data class SakWrapper<T>(val soknad: Søknad, val søknad: T)

enum class SøknadType {
    OVERGANGSSTØNAD,
    BARNETILSYN
}

object SakMapper {

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

    fun pakkOppOvergangsstønad(søknad: Søknad): SakWrapper<SøknadOvergangsstønad> {
        return pakkOpp(søknad, SøknadType.OVERGANGSSTØNAD)
    }

    fun pakkOppBarnetisyn(søknad: Søknad): SakWrapper<SøknadBarnetilsyn> {
        return pakkOpp(søknad, SøknadType.BARNETILSYN)
    }

    private inline fun <reified T> pakkOpp(søknad: Søknad,
                                           søknadType: SøknadType): SakWrapper<T> {
        if (søknad.type != søknadType) error("Feil type søknad ${søknad.id} ${søknad.type}")
        return SakWrapper(søknad, objectMapper.readValue(søknad.søknad))
    }

}
