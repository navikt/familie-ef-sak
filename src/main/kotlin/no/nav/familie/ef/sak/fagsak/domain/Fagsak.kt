package no.nav.familie.ef.sak.fagsak.domain

import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.util.UUID

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
        return søkerIdenter.maxByOrNull { it.sporbar.endret.endretTid }?.ident ?: error("Fant ingen ident på fagsak $id")
    }

    fun erAktivIdent(personIdent: String): Boolean = hentAktivIdent() == personIdent

    fun fagsakMedOppdatertGjeldendeIdent(gjeldendePersonIdent: String): Fagsak {
        val fagsakPersonForGjeldendeIdent: FagsakPerson = this.søkerIdenter.find { it.ident == gjeldendePersonIdent }?.let {
            it.copy(sporbar = it.sporbar.copy(endret = Endret()))
        } ?: FagsakPerson(ident = gjeldendePersonIdent)
        val søkerIdenterUtenGjeldende = this.søkerIdenter.filter { it.ident != gjeldendePersonIdent }

        return this.copy(søkerIdenter = søkerIdenterUtenGjeldende.toSet() + fagsakPersonForGjeldendeIdent)
    }
}

enum class Stønadstype {
    OVERGANGSSTØNAD,
    BARNETILSYN,
    SKOLEPENGER
}
