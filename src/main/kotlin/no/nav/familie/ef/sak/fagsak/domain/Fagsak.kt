package no.nav.familie.ef.sak.fagsak.domain

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

data class Fagsaker(
    val overgangsstønad: Fagsak?,
    val barnetilsyn: Fagsak?,
    val skolepenger: Fagsak?,
)

data class Fagsak(
    val id: UUID,
    val fagsakPersonId: UUID,
    val personIdenter: Set<PersonIdent>,
    val eksternId: Long,
    val stønadstype: StønadType,
    val migrert: Boolean,
    val sporbar: Sporbar,
) {
    fun erAktivIdent(personIdent: String): Boolean = hentAktivIdent() == personIdent

    fun hentAktivIdent(): String {
        return personIdenter.maxByOrNull { it.sporbar.endret.endretTid }?.ident ?: error("Fant ingen ident på fagsak $id")
    }
}

@Table("fagsak")
data class FagsakDomain(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakPersonId: UUID,
    val eksternId: Long = 0,
    @Column("stonadstype")
    val stønadstype: StønadType,
    val migrert: Boolean = false,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

fun FagsakDomain.tilFagsakMedPerson(personIdenter: Set<PersonIdent>): Fagsak =
    Fagsak(
        id = id,
        fagsakPersonId = fagsakPersonId,
        personIdenter = personIdenter,
        eksternId = eksternId,
        stønadstype = stønadstype,
        migrert = migrert,
        sporbar = sporbar,
    )
