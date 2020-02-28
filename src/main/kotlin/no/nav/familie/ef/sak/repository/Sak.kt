package no.nav.familie.ef.sak.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.familie.kontrakter.ef.søknad.Søknad
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*
import no.nav.familie.kontrakter.ef.sak.Sak as DtoSak


data class Sak(@Id
               val id: UUID? = null,
               @Column("soknad")
               val søknad: ByteArray,
               val saksnummer: String,
               @Column("journalpost_id")
               val journalpostId: String,
               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
               val sporbar: Sporbar = Sporbar())

object SakMapper {
    fun toDomain(sak: DtoSak): Sak {
        return Sak(søknad = objectMapper.writeValueAsBytes(sak.søknad),
                   saksnummer = sak.saksnummer,
                   journalpostId = sak.journalpostId)
    }

    fun toDto(sak: Sak): DtoSak {
        return DtoSak(objectMapper.readValue(sak.søknad, Søknad::class.java),
                      sak.saksnummer,
                      sak.journalpostId)
    }
}