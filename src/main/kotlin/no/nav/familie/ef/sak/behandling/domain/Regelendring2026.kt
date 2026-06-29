package no.nav.familie.ef.sak.behandling.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("regelendring_2026")
data class Regelendring2026(
    @Id
    @Column("behandling_id")
    val behandlingId: UUID,
    val begrunnelse: String,
)
