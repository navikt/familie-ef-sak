package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.relational.core.mapping.Embedded

data class FagsakPerson(val ident: String,
                        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                        val sporbar: Sporbar = Sporbar())


