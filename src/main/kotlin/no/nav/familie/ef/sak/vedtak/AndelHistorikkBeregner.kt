package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import java.time.LocalDate
import java.util.LinkedList
import java.util.UUID

enum class HistorikkType {
    VANLIG,
    FJERNET,
    ENDRET,
    ENDRING_I_INNTEKT // mindre endring i inntekt som ikke endrer beløp
}

data class AndelHistorikk(val behandlingId: UUID,
                          val vedtaksdato: LocalDate,
                          val andel: AndelTilkjentYtelse,
                          val type: HistorikkType,
                          val endretI: UUID?)

object AndelHistorikkBeregner {

    private class AndelHistorikkHolder(val behandlingId: UUID,
                                       val vedtaksdato: LocalDate,
                                       var andel: AndelTilkjentYtelse,
                                       var type: HistorikkType,
                                       var endretI: UUID?,
                                       var kontrollert: UUID)

    private fun AndelTilkjentYtelse.endring(other: AndelTilkjentYtelse): HistorikkType? {
        return when {
            this.stønadTom != other.stønadTom || this.beløp != other.beløp -> HistorikkType.ENDRET
            this.inntekt != other.inntekt -> HistorikkType.ENDRING_I_INNTEKT
            else -> null
        }
    }

    fun lagHistorikk(tilkjentYtelser: List<TilkjentYtelse>): List<AndelHistorikk> {
        val result = LinkedList(listOf<AndelHistorikkHolder>())

        tilkjentYtelser.forEach { tilkjentYtelse ->
            tilkjentYtelse.andelerTilkjentYtelse.forEach { andel ->
                val tidligereAndel = finnTidligereAndel(result, andel)
                if (tidligereAndel == null) {
                    val index = result.indexOfFirst { it.andel.stønadFom.isAfter(andel.stønadTom) }
                    val nyHolder = nyHolder(tilkjentYtelse, andel)
                    if (index == -1) {
                        result.add(nyHolder)
                    } else {
                        result.add(index, nyHolder)
                    }
                } else {
                    val endringType = tidligereAndel.andel.endring(andel)
                    if (endringType != null) {
                        tidligereAndel.andel = andel //.copy(kildeBehandlingId = tidligereAndel.andel.kildeBehandlingId)
                        tidligereAndel.type = endringType
                        tidligereAndel.endretI = tilkjentYtelse.behandlingId
                    }
                    tidligereAndel.kontrollert = tilkjentYtelse.id
                }
            }

            result.filterNot { alleredeFjernetEllerKontrollert(it, tilkjentYtelse) }.forEach {
                it.type = HistorikkType.FJERNET
                it.endretI = tilkjentYtelse.behandlingId
            }
        }

        return result.map {
            AndelHistorikk(it.behandlingId, it.vedtaksdato, it.andel, it.type, it.endretI)
        }
    }

    private fun alleredeFjernetEllerKontrollert(holder: AndelHistorikkHolder,
                                                tilkjentYtelse: TilkjentYtelse) =
            holder.type == HistorikkType.FJERNET || holder.kontrollert == tilkjentYtelse.id

    private fun nyHolder(tilkjentYtelse: TilkjentYtelse,
                         andel: AndelTilkjentYtelse) =
            AndelHistorikkHolder(tilkjentYtelse.behandlingId,
                                 tilkjentYtelse.vedtaksdato!!,
                                 andel,
                                 HistorikkType.VANLIG,
                                 null,
                                 tilkjentYtelse.id)

    private fun finnTidligereAndel(result: MutableList<AndelHistorikkHolder>,
                                   andel: AndelTilkjentYtelse) =
            result.find { it.type != HistorikkType.FJERNET && it.andel.stønadFom == andel.stønadFom }
}