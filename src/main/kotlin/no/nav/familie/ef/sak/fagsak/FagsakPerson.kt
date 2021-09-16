package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.repository.domain.Sporbar
import org.springframework.data.relational.core.mapping.Embedded

data class FagsakPerson(val ident: String,
                        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                        val sporbar: Sporbar = Sporbar())


