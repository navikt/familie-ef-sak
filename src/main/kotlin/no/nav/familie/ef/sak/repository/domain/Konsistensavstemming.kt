package no.nav.familie.ef.sak.repository.domain;

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table("konsistenavstemming")
data class Konsistensavstemming(
        @Id
        val id: Long = 0,
        val dato: LocalDate,
        @Column("stonadstype")
        val stønadstype: Stønadstype,
)
