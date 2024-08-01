package no.nav.familie.ef.sak.fagsak.dto

import java.util.UUID

class FagsakPersonDto(
    val id: UUID,
    val overgangsstønad: FagsakDto?,
    val barnetilsyn: FagsakDto?,
    val skolepenger: FagsakDto?,
)
