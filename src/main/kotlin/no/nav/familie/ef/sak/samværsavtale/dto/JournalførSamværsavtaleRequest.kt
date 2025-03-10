package no.nav.familie.ef.sak.samværsavtale.dto

import no.nav.familie.ef.sak.samværsavtale.domain.Samværsuke

data class JournalførSamværsavtaleRequest(
    val personIdent: String,
    val uker: List<Samværsuke>,
)
