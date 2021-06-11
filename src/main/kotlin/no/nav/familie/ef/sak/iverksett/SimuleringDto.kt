package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.Periodetype
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
        val andelerTilkjentYtelse: List<AndelTilkjentYtelseDto>)


fun TilkjentYtelse.tilIverksett(): TilkjentYtelseForIverksett {
    return TilkjentYtelseForIverksett(id = this.id,
                                      andelerTilkjentYtelse = this.andelerTilkjentYtelse.map { it.tilIverksettDto() },
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
