package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDto
import no.nav.familie.ef.sak.mapper.tilDto
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import java.time.LocalDate
import java.util.UUID

enum class EndringType {
    FJERNET,
    ENDRET,
    ENDRING_I_INNTEKT // mindre endring i inntekt som ikke endrer beløp
}

data class AndelHistorikkDto(val behandlingId: UUID,
                             val vedtaksdato: LocalDate, // TODO burde denne være datotid sånn att man kan vise tiden den ble opprettet?
                             val saksbehandler: String,
                             val andel: AndelTilkjentYtelseDto,
                             val endring: HistorikkEndring?)

data class HistorikkEndring(val type: EndringType,
                            val behandlingId: UUID,
                            val vedtaksdato: LocalDate)

object AndelHistorikkBeregner {

    private class AndelHistorikkHolder(val behandlingId: UUID,
                                       val vedtaksdato: LocalDate,
                                       val saksbehandler: String,
                                       var andel: AndelTilkjentYtelse,
                                       var endring: HistorikkEndring?,
                                       var kontrollert: UUID)

    private fun AndelTilkjentYtelse.endring(other: AndelTilkjentYtelse): EndringType? {
        return when {
            this.stønadTom != other.stønadTom || this.beløp != other.beløp -> EndringType.ENDRET
            this.inntekt != other.inntekt -> EndringType.ENDRING_I_INNTEKT
            else -> null
        }
    }

    fun lagHistorikk(tilkjentYtelser: List<TilkjentYtelse>): List<AndelHistorikkDto> {
        val result = mutableListOf<AndelHistorikkHolder>()

        sorterTilkjentYtelser(tilkjentYtelser).forEach { tilkjentYtelse ->
            tilkjentYtelse.andelerTilkjentYtelse.forEach { andel ->
                val tidligereAndel = finnTilsværendeAndelITidiligereBehandlinger(result, andel)
                if (tidligereAndel == null) {
                    val index = finnIndeksForPeriodeSomErEtterAndel(result, andel)
                    result.add(index, nyAndel(tilkjentYtelse, andel))
                } else {
                    val endringType = tidligereAndel.andel.endring(andel)
                    if (endringType != null) {
                        tidligereAndel.andel = andel //.copy(kildeBehandlingId = tidligereAndel.andel.kildeBehandlingId)
                        tidligereAndel.endring = lagEndring(endringType, tilkjentYtelse)
                    }
                    tidligereAndel.kontrollert = tilkjentYtelse.id
                }
            }

            result.filterNot { alleredeFjernetEllerKontrollert(it, tilkjentYtelse) }.forEach {
                it.endring = lagEndring(EndringType.FJERNET, tilkjentYtelse)
            }
        }

        return result.map {
            AndelHistorikkDto(it.behandlingId, it.vedtaksdato, it.saksbehandler, it.andel.tilDto(), it.endring)
        }
    }

    private fun sorterTilkjentYtelser(tilkjentYtelser: List<TilkjentYtelse>): List<TilkjentYtelse> =
            tilkjentYtelser.sortedBy { it.sporbar.opprettetTid }
                    .map { it.copy(andelerTilkjentYtelse = it.andelerTilkjentYtelse.sortedBy(AndelTilkjentYtelse::stønadFom)) }

    /**
     * Finner indeks for andelen etter [andel], hvis den ikke finnes returneres [result] sin size
     */
    private fun finnIndeksForPeriodeSomErEtterAndel(result: List<AndelHistorikkHolder>,
                                                    andel: AndelTilkjentYtelse): Int {
        val index = result.indexOfFirst { it.andel.stønadFom.isAfter(andel.stønadTom) }
        return if (index == -1) result.size else index
    }

    private fun lagEndring(type: EndringType, tilkjentYtelse: TilkjentYtelse) =
            HistorikkEndring(type = type,
                             behandlingId = tilkjentYtelse.behandlingId,
                             vedtaksdato = tilkjentYtelse.vedtaksdato!!)

    private fun alleredeFjernetEllerKontrollert(holder: AndelHistorikkHolder,
                                                tilkjentYtelse: TilkjentYtelse) =
            holder.endring?.type == EndringType.FJERNET || holder.kontrollert == tilkjentYtelse.id

    private fun nyAndel(tilkjentYtelse: TilkjentYtelse,
                        andel: AndelTilkjentYtelse) =
            AndelHistorikkHolder(tilkjentYtelse.behandlingId,
                                 tilkjentYtelse.vedtaksdato!!,
                                 tilkjentYtelse.sporbar.opprettetAv,
                                 andel,
                                 null,
                                 tilkjentYtelse.id)

    private fun finnTilsværendeAndelITidiligereBehandlinger(result: MutableList<AndelHistorikkHolder>,
                                                            andel: AndelTilkjentYtelse) =
            result.find { it.endring?.type != EndringType.FJERNET && it.andel.stønadFom == andel.stønadFom }
}