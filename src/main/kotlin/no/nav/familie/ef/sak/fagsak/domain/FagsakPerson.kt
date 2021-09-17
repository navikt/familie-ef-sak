package no.nav.familie.ef.sak.fagsak.domain

import no.nav.familie.ef.sak.felles.domain.Sporbar
import org.springframework.data.relational.core.mapping.Embedded

data class FagsakPerson(val ident: String,
                        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                        val sporbar: Sporbar = Sporbar())


