package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.fagsak.domain.Stønadstype

data class FagsakRequest(val personIdent: String,
                         val stønadstype: Stønadstype)