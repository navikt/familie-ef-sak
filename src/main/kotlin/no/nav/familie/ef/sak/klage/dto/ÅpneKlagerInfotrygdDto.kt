package no.nav.familie.ef.sak.klage.dto

import no.nav.familie.kontrakter.felles.ef.StønadType

data class ÅpneKlagerInfotrygdDto(
    val stønadstyper: Set<StønadType>,
)
