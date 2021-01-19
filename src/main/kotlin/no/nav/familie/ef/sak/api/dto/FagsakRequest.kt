package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.repository.domain.Stønadstype

data class FagsakRequest(val personIdent: String,
                         val stønadstype: Stønadstype)