package no.nav.familie.ef.sak.api.fagsak

import no.nav.familie.ef.sak.repository.domain.Stønadstype

data class FagsakRequest(
        val personIdent: String,
        val stønadstype: Stønadstype
)