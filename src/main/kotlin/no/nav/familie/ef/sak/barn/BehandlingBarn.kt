package no.nav.familie.ef.sak.barn

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table("behandling_barn")
data class BehandlingBarn(@Id
                          val id: UUID = UUID.randomUUID(),
                          val behandlingId: UUID,
                          val personIdent: String?,
                          val navn: String?,
                          @Column("fodsel_termindato")
                          val fødselTermindato: LocalDate? = null,
)
