package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.kontrakter.ef.sak.SakRequest
import no.nav.familie.kontrakter.ef.søknad.Søknad
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("sak")
data class Sak(@Id
               val id: UUID = UUID.randomUUID(),
               @Column("soknad")
               val søknad: Søknad,
               val saksnummer: String,
               @Column("journalpost_id")
               val journalpostId: String,
               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
               val sporbar: Sporbar = Sporbar(),
               val søker: Søker,
               val barn: Set<Barn>)

object SakMapper {
    fun toDomain(sak: SakRequest): Sak {
        return Sak(søknad = sak.søknad.søknad,
                   saksnummer = sak.saksnummer,
                   journalpostId = sak.journalpostId,
                   søker = SøkerMapper.toDomain(sak.søknad.søknad),
                   barn = BarnMapper.toDomain(sak.søknad.søknad))
    }

}
