package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Endret
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseType
import no.nav.familie.kontrakter.ef.felles.StønadType
import java.time.LocalDate
import java.util.UUID


data class SimuleringDto(
        val nyTilkjentYtelseMedMetaData: TilkjentYtelseForIverksettMedMetadata,
        val forrigeTilkjentYtelse: TilkjentYtelseForIverksett?
)


data class TilkjentYtelseForIverksettMedMetadata(val tilkjentYtelse: TilkjentYtelseForIverksett,
                                                 val saksbehandlerId: String,
                                                 val eksternBehandlingId: Long,
                                                 val stønadstype: Stønadstype,
                                                 val eksternFagsakId: Long,
                                                 val personIdent: String,
                                                 val behandlingId: UUID,
                                                 val vedtaksdato: LocalDate)

data class TilkjentYtelseForIverksett(
        val id: UUID = UUID.randomUUID(),
        val vedtaksdato: LocalDate? = null,
        val status: TilkjentYtelseStatus,
        val andelerTilkjentYtelse: List<AndelTilkjentYtelseForIverksett>)

data class AndelTilkjentYtelseForIverksett(val periodebeløp: PeriodebeløpDto,
                                           val periodeId: Long? = null,
                                           val forrigePeriodeId: Long? = null,
                                           val kildeBehandlingId: UUID? = null)

data class PeriodebeløpDto(val beløp: Int,
                           var periodetype: Periodetype,
                           val fraOgMed: LocalDate,
                           val inntekt: Int,
                           val inntektsreduksjon: Int,
                           val samordningsfradrag: Int,
                           val tilOgMed: LocalDate)

enum class Periodetype {
    MÅNED
}


fun TilkjentYtelseForIverksettMedMetadata.tilTilkjentYtelse(status: TilkjentYtelseStatus = TilkjentYtelseStatus.OPPRETTET): TilkjentYtelse {

    return TilkjentYtelse(vedtaksdato = vedtaksdato,
                          status = status,
                          andelerTilkjentYtelse = this.tilAndelerTilkjentYtelse(),
                          id = this.tilkjentYtelse.id,
                          behandlingId = this.behandlingId,
                          personident = this.personIdent,
                          utbetalingsoppdrag = null)

}

fun TilkjentYtelseForIverksettMedMetadata.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> {

    return this.tilkjentYtelse.andelerTilkjentYtelse
            .map {
                AndelTilkjentYtelse(beløp = it.periodebeløp.beløp,
                                    stønadFom = it.periodebeløp.fraOgMed,
                                    stønadTom = it.periodebeløp.tilOgMed,
                                    personIdent = this.personIdent,
                                    samordningsfradrag = it.periodebeløp.samordningsfradrag,
                                    inntektsreduksjon = it.periodebeløp.inntektsreduksjon,
                                    inntekt = it.periodebeløp.inntekt
                )
            }
}

fun TilkjentYtelse.tilIverksett(): TilkjentYtelseForIverksett {
    return TilkjentYtelseForIverksett(id = this.id,
                                      andelerTilkjentYtelse = this.andelerTilkjentYtelse.map { it.tilIverksett() },
                                      vedtaksdato = this.vedtaksdato,
                                      status = this.status)
}

fun TilkjentYtelse.tilIverksettMedMetaData(saksbehandlerId: String,
                                           eksternBehandlingId: Long,
                                           stønadstype: Stønadstype,
                                           eksternFagsakId: Long): TilkjentYtelseForIverksettMedMetadata {
    return TilkjentYtelseForIverksettMedMetadata(tilkjentYtelse = this.tilIverksett(),
                                                 saksbehandlerId = saksbehandlerId,
                                                 eksternBehandlingId = eksternBehandlingId,
                                                 stønadstype = stønadstype,
                                                 eksternFagsakId = eksternFagsakId,
                                                 personIdent = this.personident,
                                                 behandlingId = this.behandlingId,
                                                 vedtaksdato = this.vedtaksdato ?: LocalDate.now())
}

fun AndelTilkjentYtelse.tilIverksett(): AndelTilkjentYtelseForIverksett {
    return AndelTilkjentYtelseForIverksett(kildeBehandlingId = this.kildeBehandlingId
                                                               ?: error("Savner kildeBehandlingId på andel med periodeId=${this.periodeId}"),
                                           periodebeløp = PeriodebeløpDto(beløp = this.beløp,
                                                                          periodetype = Periodetype.MÅNED,
                                                                          fraOgMed = this.stønadFom,
                                                                          inntektsreduksjon = this.inntektsreduksjon,
                                                                          samordningsfradrag = this.samordningsfradrag,
                                                                          inntekt = this.inntekt,
                                                                          tilOgMed = this.stønadTom),
                                           periodeId = null,
                                           forrigePeriodeId = null)
}

