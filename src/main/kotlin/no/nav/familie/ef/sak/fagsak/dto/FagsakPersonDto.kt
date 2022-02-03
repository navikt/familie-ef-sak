package no.nav.familie.ef.sak.fagsak.dto

import java.util.UUID

class FagsakPersonDto(
        val id: UUID,
        val overgangsstønad: UUID?,
        val barnetilsyn: UUID?,
        val skolepenger: UUID?,
)

class FagsakPersonMedBehandlingerDto(
        val id: UUID,
        val overgangsstønad: FagsakDto?,
        val barnetilsyn: FagsakDto?,
        val skolepenger: FagsakDto?,
)
