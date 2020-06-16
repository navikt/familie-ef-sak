package no.nav.familie.ef.sak.integration.dto.pdl

data class PdlPersonRequest<T>(val variables: T,
                               val query: String)