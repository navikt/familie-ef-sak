package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.api.dto.SakDto
import no.nav.familie.kontrakter.ef.sak.SakRequest
import no.nav.familie.kontrakter.ef.søknad.Søknad
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("sak")
data class Sak(@Id
               val id: UUID = UUID.randomUUID(),
               @Column("soknad")
               val søknad: ByteArray,
               val saksnummer: String,
               @Column("journalpost_id")
               val journalpostId: String,
               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
               val sporbar: Sporbar = Sporbar(),
               val søker: Søker,
               val barn: Set<Barn>)

object SakMapper {
    fun toDomain(sak: SakRequest): Sak {
        return Sak(søknad = objectMapper.writeValueAsBytes(sak.søknad),
                   saksnummer = sak.saksnummer,
                   journalpostId = sak.journalpostId,
                   søker = SøkerMapper.toDomain(sak.søknad.søknad),
                   barn = BarnMapper.toDomain(sak.søknad.søknad))
    }

    fun toDto(sak: Sak): SakDto {
        return SakDto(objectMapper.readValue(sak.søknad, Søknad::class.java),
                      sak.saksnummer,
                      sak.journalpostId)
    }
}
