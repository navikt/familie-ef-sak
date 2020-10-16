package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table

@Table("behandling_journalpost")
data class BehandlingJournalpost(@Column("journalpost_id") val journalpostId: String,
                                 @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                 val sporbar: Sporbar = Sporbar())


