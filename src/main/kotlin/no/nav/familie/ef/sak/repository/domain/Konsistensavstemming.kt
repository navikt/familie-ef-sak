package no.nav.familie.ef.sak.repository.domain;

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.util.*

data class Konsistensavstemming(
        @Id
        val id: UUID = UUID.randomUUID(),
        val dato: LocalDate,
        @Column("stonadstype")
        val stønadstype: Stønadstype,
)
