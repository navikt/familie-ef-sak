package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import org.springframework.data.relational.core.mapping.Embedded

data class Behandlingsjournalpost(val journalpostId: String,
                                  val journalpostType: Journalposttype,
                                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                  val sporbar: Sporbar = Sporbar())


