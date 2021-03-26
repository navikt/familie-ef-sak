package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Behandlingsjournalpost(@Id
                                  val behandlingId: UUID,
                                  val journalpostId: String,
                                  val journalpostType: Journalposttype,
                                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                  val sporbar: Sporbar = Sporbar())


