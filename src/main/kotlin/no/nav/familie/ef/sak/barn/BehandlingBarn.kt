package no.nav.familie.ef.sak.barn

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

data class BehandlingBarn(@Id
                          val id: UUID = UUID.randomUUID(),
                          val behandlingId: UUID,
                          @Column("soknad_barn_id")
                          val søknadBarnId: UUID,
                          val personIdent: String?,
                          val navn: String?,
                          @Column("fodsel_termindato")
                          val fødselTermindato: LocalDate? = null,
)
