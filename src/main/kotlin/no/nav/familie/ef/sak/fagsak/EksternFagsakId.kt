package no.nav.familie.ef.sak.fagsak

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("fagsak_ekstern")
data class EksternFagsakId(@Id
                           val id: Long = 0)