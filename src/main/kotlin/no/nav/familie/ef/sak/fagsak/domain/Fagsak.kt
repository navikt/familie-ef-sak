package no.nav.familie.ef.sak.fagsak.domain

import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

data class Fagsak(
        val id: UUID,
        val fagsakPersonId: UUID,
        val personIdenter: Set<PersonIdent>,
        val eksternId: EksternFagsakId,
        val stønadstype: Stønadstype,
        val migrert: Boolean,
        val søkerIdenter: Set<FagsakPersonOld>,
        val sporbar: Sporbar
) {
    fun erAktivIdent(personIdent: String): Boolean = hentAktivIdent() == personIdent

    fun hentAktivIdent(): String {
        return søkerIdenter.maxByOrNull { it.sporbar.endret.endretTid }?.ident ?: error("Fant ingen ident på fagsak $id")
    }
}

@Table("fagsak")
data class FagsakDao(@Id
                     val id: UUID = UUID.randomUUID(),
                     val fagsakPersonId: UUID,
                     @MappedCollection(idColumn = "fagsak_id")
                     val eksternId: EksternFagsakId = EksternFagsakId(),
                     @Column("stonadstype")
                     val stønadstype: Stønadstype,
                     val migrert: Boolean = false,
                     @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                     val sporbar: Sporbar = Sporbar(),
                     @MappedCollection(idColumn = "fagsak_id")
                     val søkerIdenter: Set<FagsakPersonOld> = setOf()) {

    fun hentAktivIdent(): String {
        return søkerIdenter.maxByOrNull { it.sporbar.endret.endretTid }?.ident ?: error("Fant ingen ident på fagsak $id")
    }

    fun erAktivIdent(personIdent: String): Boolean = hentAktivIdent() == personIdent

    fun fagsakMedOppdatertGjeldendeIdent(gjeldendePersonIdent: String): FagsakDao {
        val fagsakPersonForGjeldendeIdent: FagsakPersonOld = this.søkerIdenter.find { it.ident == gjeldendePersonIdent }?.let {
            it.copy(sporbar = it.sporbar.copy(endret = Endret()))
        } ?: FagsakPersonOld(ident = gjeldendePersonIdent)
        val søkerIdenterUtenGjeldende = this.søkerIdenter.filter { it.ident != gjeldendePersonIdent }

        return this.copy(søkerIdenter = søkerIdenterUtenGjeldende.toSet() + fagsakPersonForGjeldendeIdent)
    }
}

fun FagsakDao.tilFagsak(personIdenter: Set<PersonIdent>): Fagsak =
        Fagsak(
                id = id,
                fagsakPersonId = fagsakPersonId,
                personIdenter = personIdenter,
                eksternId = eksternId,
                stønadstype = stønadstype,
                migrert = migrert,
                søkerIdenter = søkerIdenter,
                sporbar = sporbar
        )

enum class Stønadstype {
    OVERGANGSSTØNAD,
    BARNETILSYN,
    SKOLEPENGER
}
