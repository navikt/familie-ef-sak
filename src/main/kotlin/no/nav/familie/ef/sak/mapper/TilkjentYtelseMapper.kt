package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDto
import no.nav.familie.ef.sak.api.dto.OldAndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.OldTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDto
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus

fun OldTilkjentYtelseDTO.tilTilkjentYtelse(status: TilkjentYtelseStatus = TilkjentYtelseStatus.OPPRETTET): TilkjentYtelse {

    return TilkjentYtelse(behandlingId = behandlingId,
                          personident = søker,
                          vedtaksdato = vedtaksdato,
                          status = status,
                          andelerTilkjentYtelse = tilAndelerTilkjentYtelse())
}

fun OldTilkjentYtelseDTO.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> {

    return this.andelerTilkjentYtelse
            .map {
                AndelTilkjentYtelse(beløp = it.beløp,
                                    stønadFom = it.stønadFom,
                                    stønadTom = it.stønadTom,
                                    inntekt = it.inntekt,
                                    inntektsreduksjon = it.inntektsreduksjon,
                                    samordningsfradrag = it.samordningsfradrag,
                                    personIdent = it.personIdent)
            }
}

fun TilkjentYtelse.tilOldDto(): OldTilkjentYtelseDTO {
    return OldTilkjentYtelseDTO(id = this.id,
                                behandlingId = this.behandlingId,
                                søker = this.personident,
                                andelerTilkjentYtelse = this.andelerTilkjentYtelse.map { it.tilOldDto() })
}

fun TilkjentYtelse.tilDto(): TilkjentYtelseDto {
    return TilkjentYtelseDto(behandlingId = this.behandlingId,
                             vedtaksdato = this.vedtaksdato,
                             andeler = this.andelerTilkjentYtelse.map { andel ->
                                 AndelTilkjentYtelseDto(beløp = andel.beløp,
                                                        stønadFra = andel.stønadFom,
                                                        stønadTil = andel.stønadTom,
                                                        inntekt = andel.inntekt,
                                                        samordningsfradrag = andel.samordningsfradrag)
                             })
}

fun AndelTilkjentYtelse.tilOldDto(): OldAndelTilkjentYtelseDTO {
    return OldAndelTilkjentYtelseDTO(beløp = this.beløp,
                                     stønadFom = this.stønadFom,
                                     stønadTom = this.stønadTom,
                                     kildeBehandlingId = this.kildeBehandlingId
                                                      ?: error("Savner kildeBehandlingId på andel med periodeId=${this.periodeId}"),
                                     inntekt = this.inntekt,
                                     inntektsreduksjon  = this.inntektsreduksjon,
                                     samordningsfradrag = this.samordningsfradrag,
                                     personIdent = this.personIdent)
}
