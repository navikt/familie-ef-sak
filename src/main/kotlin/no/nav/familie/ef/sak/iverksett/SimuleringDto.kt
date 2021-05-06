package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.repository.domain.stønadFom
import no.nav.familie.ef.sak.repository.domain.stønadTom
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
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
                                                 val eksternFagsakId: Long)

data class TilkjentYtelseForIverksett(
        val id: UUID = UUID.randomUUID(),
        val behandlingId: UUID,
        val personident: String,
        val stønadFom: LocalDate? = null,
        val stønadTom: LocalDate? = null,
        val opphørFom: LocalDate? = null,
        val utbetalingsoppdrag: Utbetalingsoppdrag? = null,
        val vedtaksdato: LocalDate? = null,
        val status: TilkjentYtelseStatus,
        val type: TilkjentYtelseType,
        val andelerTilkjentYtelse: List<AndelTilkjentYtelseForIverksett>)

data class AndelTilkjentYtelseForIverksett(val periodebeløp: PeriodebeløpDto,
                                           val personIdent: String,
                                           val periodeId: Long? = null,
                                           val forrigePeriodeId: Long? = null,
                                           val kildeBehandlingId: UUID? = null)

data class PeriodebeløpDto(val utbetaltPerPeriode: Int,
                           var periodetype: Periodetype,
                           val fraOgMed: LocalDate,
                           val tilOgMed: LocalDate)

enum class Periodetype {
    MÅNED
}


fun TilkjentYtelseForIverksett.tilTilkjentYtelse(status: TilkjentYtelseStatus = TilkjentYtelseStatus.OPPRETTET): TilkjentYtelse {

    return TilkjentYtelse(behandlingId = behandlingId,
                          personident = personident,
                          vedtaksdato = vedtaksdato,
                          status = status,
                          andelerTilkjentYtelse = tilAndelerTilkjentYtelse())
}

fun TilkjentYtelseForIverksett.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> {

    return this.andelerTilkjentYtelse
            .map {
                AndelTilkjentYtelse(beløp = it.periodebeløp.utbetaltPerPeriode,
                                    stønadFom = it.periodebeløp.fraOgMed,
                                    stønadTom = it.periodebeløp.tilOgMed,
                                    personIdent = it.personIdent)
            }
}

fun TilkjentYtelse.tilIverksett(): TilkjentYtelseForIverksett {
    return TilkjentYtelseForIverksett(id = this.id,
                                      behandlingId = this.behandlingId,
                                      andelerTilkjentYtelse = this.andelerTilkjentYtelse.map { it.tilIverksett() },
                                      personident = this.personident,
                                      stønadFom = this.stønadFom(),
                                      stønadTom = this.stønadTom(),
                                      opphørFom = this.opphørsdato,
                                      utbetalingsoppdrag = null,
                                      vedtaksdato = this.vedtaksdato,
                                      status = this.status,
                                      type = this.type)
}

fun TilkjentYtelse.tilIverksettMedMetaData(saksbehandlerId: String,
                                           eksternBehandlingId: Long,
                                           stønadstype: Stønadstype,
                                           eksternFagsakId: Long): TilkjentYtelseForIverksettMedMetadata {
    return TilkjentYtelseForIverksettMedMetadata(tilkjentYtelse = this.tilIverksett(),
                                                 saksbehandlerId = saksbehandlerId,
                                                 eksternBehandlingId = eksternBehandlingId,
                                                 stønadstype = stønadstype,
                                                 eksternFagsakId = eksternFagsakId)
}

fun AndelTilkjentYtelse.tilIverksett(): AndelTilkjentYtelseForIverksett {
    return AndelTilkjentYtelseForIverksett(kildeBehandlingId = this.kildeBehandlingId
                                                               ?: error("Savner kildeBehandlingId på andel med periodeId=${this.periodeId}"),
                                           personIdent = this.personIdent,
                                           periodebeløp = PeriodebeløpDto(utbetaltPerPeriode = this.beløp,
                                                                          periodetype = Periodetype.MÅNED,
                                                                          fraOgMed = this.stønadFom,
                                                                          tilOgMed = this.stønadTom),
                                           periodeId = null,
                                           forrigePeriodeId = null)
}

