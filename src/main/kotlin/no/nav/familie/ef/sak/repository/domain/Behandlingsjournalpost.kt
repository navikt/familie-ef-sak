package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table

@Table("behandling_journalpost")
data class Behandlingsjournalpost(@Column("journalpost_id") val journalpostId: String,
                                  @Column("journalpost_type") val journalpostType: Journalposttype,
                                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                 val sporbar: Sporbar = Sporbar())


