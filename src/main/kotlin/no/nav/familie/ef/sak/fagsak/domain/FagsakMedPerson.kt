package no.nav.familie.ef.sak.fagsak.domain

import no.nav.familie.ef.sak.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

data class Fagsaker(
        val overgangsstønad: FagsakMedPerson?,
        val barnetilsyn: FagsakMedPerson?,
        val skolepenger: FagsakMedPerson?,
)

data class FagsakMedPerson(
        val id: UUID,
        val fagsakPersonId: UUID,
        val personIdenter: Set<PersonIdent>,
        val eksternId: EksternFagsakId,
        val stønadstype: Stønadstype,
        val migrert: Boolean,
        val sporbar: Sporbar
) {

    fun erAktivIdent(personIdent: String): Boolean = hentAktivIdent() == personIdent

    fun hentAktivIdent(): String {
        return personIdenter.maxByOrNull { it.sporbar.endret.endretTid }?.ident ?: error("Fant ingen ident på fagsak $id")
    }

}

@Table("fagsak")
data class Fagsak(@Id
                  val id: UUID = UUID.randomUUID(),
                  val fagsakPersonId: UUID,
                  @MappedCollection(idColumn = "fagsak_id")
                  val eksternId: EksternFagsakId = EksternFagsakId(),
                  @Column("stonadstype")
                  val stønadstype: Stønadstype,
                  val migrert: Boolean = false,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar()) {

}

fun Fagsak.tilFagsakMedPerson(personIdenter: Set<PersonIdent>): FagsakMedPerson =
        FagsakMedPerson(
                id = id,
                fagsakPersonId = fagsakPersonId,
                personIdenter = personIdenter,
                eksternId = eksternId,
                stønadstype = stønadstype,
                migrert = migrert,
                sporbar = sporbar
        )

enum class Stønadstype {
    OVERGANGSSTØNAD,
    BARNETILSYN,
    SKOLEPENGER
}
