package no.nav.familie.ef.sak.samværsavtale.dto

import no.nav.familie.ef.sak.samværsavtale.domain.Samværsuke

data class JournalførBeregnetSamværRequest(
    val personIdent: String,
    val uker: List<Samværsuke>,
    val notat: String,
    val oppsumering: String,
)
