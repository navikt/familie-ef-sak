package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.util.*

data class Fagsak(@Id
                  val id: UUID = UUID.randomUUID(),
                  @MappedCollection(idColumn = "fagsak_id")
                  val eksternId: EksternFagsakId = EksternFagsakId(),
                  @Column("stonadstype")
                  val stønadstype: Stønadstype,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar(),
                  @MappedCollection(idColumn = "fagsak_id")
                  val søkerIdenter: Set<FagsakPerson> = setOf()) {

    fun hentAktivIdent(): String {
        return søkerIdenter.maxByOrNull { it.sporbar.opprettetTid }?.ident ?: error("Fant ingen ident på fagsak $id")
    }
}

enum class Stønadstype {
    OVERGANGSSTØNAD,
    BARNETILSYN,
    SKOLEPENGER
}

