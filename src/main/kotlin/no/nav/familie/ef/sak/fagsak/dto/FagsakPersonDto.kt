package no.nav.familie.ef.sak.fagsak.dto

import java.util.UUID

class FagsakPersonDto(
        val id: UUID,
        val overgangsstønad: UUID?,
        val barnetilsyn: UUID?,
        val skolepenger: UUID?,
)

/**
 * Inneholder id til fagsakPerson samt de ulike fagsakene til personen for å kunne vise opp på behandlingsoversikten
 */
class FagsakPersonUtvidetDto(
        val id: UUID,
        val overgangsstønad: FagsakDto?,
        val barnetilsyn: FagsakDto?,
        val skolepenger: FagsakDto?,
)
