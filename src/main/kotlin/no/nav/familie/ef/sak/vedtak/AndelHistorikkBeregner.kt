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
        val historikk = lagHistorikkHolders(sorterTilkjentYtelser(tilkjentYtelser))

        return historikk.map {
            AndelHistorikkDto(it.behandlingId, it.vedtaksdato, it.saksbehandler, it.andel.tilDto(), it.endring)
        }
    }

    private fun lagHistorikkHolders(tilkjentYtelser: List<TilkjentYtelse>): List<AndelHistorikkHolder> {
        val historikk = mutableListOf<AndelHistorikkHolder>()

        tilkjentYtelser.forEach { tilkjentYtelse ->
            tilkjentYtelse.andelerTilkjentYtelse.forEach { andel ->
                val andelFraHistorikk = finnTilsværendeAndelIHistorikk(historikk, andel)
                if (andelFraHistorikk == null) {
                    val index = finnIndeksForPeriodeSomErEtterAndel(historikk, andel)
                    historikk.add(index, nyAndel(tilkjentYtelse, andel))
                } else {
                    val endringType = andelFraHistorikk.andel.endring(andel)
                    if (endringType != null) {
                        andelFraHistorikk.andel = andel
                        andelFraHistorikk.endring = lagEndring(endringType, tilkjentYtelse)
                    }
                    andelFraHistorikk.kontrollert = tilkjentYtelse.id
                }
            }

            markerFjernede(historikk, tilkjentYtelse)
        }
        return historikk
    }

    /**
     * Hvis en [tilkjentYtelse] sin behandlingId ikke er lik andelene i historikk sine verdier for kontrollert,
     * så betyr det att selve andelen i historikken er fjernet då den ikke har blitt kontrollert i denne iterasjonen.
     * Den markeres då som fjernet.
     */
    private fun markerFjernede(historikk: MutableList<AndelHistorikkHolder>,
                               tilkjentYtelse: TilkjentYtelse) {
        historikk.filterNot { alleredeFjernetEllerKontrollert(it, tilkjentYtelse) }.forEach {
            it.endring = lagEndring(EndringType.FJERNET, tilkjentYtelse)
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
            AndelHistorikkHolder(behandlingId = tilkjentYtelse.behandlingId,
                                 vedtaksdato = tilkjentYtelse.vedtaksdato!!,
                                 saksbehandler = tilkjentYtelse.sporbar.opprettetAv,
                                 andel = andel,
                                 endring = null,
                                 kontrollert = tilkjentYtelse.id)

    private fun finnTilsværendeAndelIHistorikk(result: MutableList<AndelHistorikkHolder>,
                                               andel: AndelTilkjentYtelse): AndelHistorikkHolder? =
            result.findLast { it.endring?.type != EndringType.FJERNET && it.andel.stønadFom == andel.stønadFom }

}
