package no.nav.familie.ef.sak.behandling.domain

import org.springframework.data.relational.core.mapping.Table

@Table("behandling_ekstern")
data class EksternBehandlingId(
    val id: Long = 0,
)
