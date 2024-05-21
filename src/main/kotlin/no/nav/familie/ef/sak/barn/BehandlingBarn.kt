package no.nav.familie.ef.sak.barn

import no.nav.familie.ef.sak.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.UUID

data class BehandlingBarn(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    @Column("soknad_barn_id")
    val søknadBarnId: UUID? = null,
    val personIdent: String? = null,
    val navn: String? = null,
    @Column("fodsel_termindato")
    val fødselTermindato: LocalDate? = null,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    fun erMatchendeBarn(annetBarn: BehandlingBarn): Boolean =
        (this.personIdent != null && this.personIdent == annetBarn.personIdent) ||
            (this.søknadBarnId != null && this.søknadBarnId == annetBarn.søknadBarnId) ||
            (this.fødselTermindato != null && this.fødselTermindato == annetBarn.fødselTermindato)
}
