package no.nav.familie.ef.sak.repository

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*


class Sak(@Id
          val id: UUID? = null,
          @Column("soknad")
          val søknad: ByteArray,
          val saksnummer: String,
          @Column("journalpost_id")
          val journalpostId: String,
          @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
          val sporbar: Sporbar = Sporbar()) {


}
