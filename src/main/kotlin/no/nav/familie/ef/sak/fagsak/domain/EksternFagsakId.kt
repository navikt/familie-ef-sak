package no.nav.familie.ef.sak.fagsak.domain

import org.springframework.data.relational.core.mapping.Table

@Table("fagsak_ekstern")
data class EksternFagsakId(
    val id: Long = 0L,
)
