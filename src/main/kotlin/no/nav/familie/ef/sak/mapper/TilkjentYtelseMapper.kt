package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus

fun TilkjentYtelseDTO.tilTilkjentYtelse(status: TilkjentYtelseStatus = TilkjentYtelseStatus.OPPRETTET): TilkjentYtelse {

    return TilkjentYtelse(behandlingId = behandlingId,
                          personident = søker,
                          vedtaksdato = vedtaksdato,
                          status = status,
                          andelerTilkjentYtelse = tilAndelerTilkjentYtelse())
}

fun TilkjentYtelseDTO.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> {

    return this.andelerTilkjentYtelse
            .map {
                AndelTilkjentYtelse(beløp = it.beløp,
                                    stønadFom = it.stønadFom,
                                    stønadTom = it.stønadTom,
                                    personIdent = it.personIdent)
            }
}

fun TilkjentYtelse.tilDto(): TilkjentYtelseDTO {
    return TilkjentYtelseDTO(id = this.id,
                             behandlingId = this.behandlingId,
                             søker = this.personident,
                             andelerTilkjentYtelse = this.andelerTilkjentYtelse.map { it.tilDto() })
}

fun AndelTilkjentYtelse.tilDto(): AndelTilkjentYtelseDTO {
    return AndelTilkjentYtelseDTO(beløp = this.beløp,
                                  stønadFom = this.stønadFom,
                                  stønadTom = this.stønadTom,
                                  kildeBehandlingId = this.kildeBehandlingId
                                                      ?: error("Savner kildeBehandlingId på andel med periodeId=${this.periodeId}"),
                                  personIdent = this.personIdent)
}
