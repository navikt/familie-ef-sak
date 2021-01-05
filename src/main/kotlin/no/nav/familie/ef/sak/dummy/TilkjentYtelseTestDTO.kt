package no.nav.familie.ef.sak.dummy

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.repository.domain.Stønadstype

data class TilkjentYtelseTestDTO(val nyTilkjentYtelse: TilkjentYtelseDTO,
                                 val stønadstype: Stønadstype)