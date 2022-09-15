package no.nav.familie.ef.sak.klage.dto

import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto

data class KlagebehandlingerDto(
    val overgangsstønad: List<KlagebehandlingDto>,
    val barnetilsyn: List<KlagebehandlingDto>,
    val skolepenger: List<KlagebehandlingDto>
)
