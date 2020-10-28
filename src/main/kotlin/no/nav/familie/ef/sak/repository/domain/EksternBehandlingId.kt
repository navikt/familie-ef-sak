package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("behandling_ekstern")
data class EksternBehandlingId (
        @Id
        @Column("ekstern_id")
        val eksternId: Long
)