package no.nav.familie.ef.sak.fagsak

import no.nav.familie.kontrakter.felles.ef.StønadType

data class FagsakRequest(val personIdent: String,
                         val stønadstype: StønadType)