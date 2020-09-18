package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("fagsak_person")
data class FagsakPerson(@Id
                        val id: UUID = UUID.randomUUID(),
                        val ident: String,
                        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                        val sporbar: Sporbar = Sporbar())


