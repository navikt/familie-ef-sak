package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.tilkjentytelse.AndelTilkjentYtelseDto
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.tilDto
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import java.time.LocalDateTime
import java.util.UUID

enum class EndringType {
    FJERNET,
    ENDRET,
    ENDRING_I_INNTEKT // mindre endring i inntekt som ikke endrer beløp
}

data class AndelHistorikkDto(val behandlingId: UUID,
                             val behandlingType: BehandlingType,
                             val vedtakstidspunkt: LocalDateTime,
                             val saksbehandler: String,
                             val andel: AndelTilkjentYtelseDto,
                             val aktivitet: AktivitetType,
                             val periodeType: VedtaksperiodeType,
                             val endring: HistorikkEndring?)

data class HistorikkEndring(val type: EndringType,
                            val behandlingId: UUID,
                            val vedtakstidspunkt: LocalDateTime)

object AndelHistorikkBeregner {

    /**
     * @param kontrollert brukes for å sjekke om en andel er fjernet eller ikke
     */
    private class AndelHistorikkHolder(val behandlingId: UUID,
                                       val vedtakstidspunkt: LocalDateTime,
                                       val saksbehandler: String,
                                       var andel: AndelTilkjentYtelse,
                                       var endring: HistorikkEndring?,
                                       var vedtaksperiode: Vedtaksperiode,
                                       var kontrollert: UUID)

    fun lagHistorikk(tilkjentYtelser: List<TilkjentYtelse>,
                     vedtaksliste: List<Vedtak>,
                     behandlinger: List<Behandling>): List<AndelHistorikkDto> {
        val historikk = lagHistorikkHolders(sorterTilkjentYtelser(tilkjentYtelser), vedtaksliste)
        val behandlingerPåId = behandlinger.associate { it.id to it.type }

        return historikk.map {
            AndelHistorikkDto(behandlingId = it.behandlingId,
                              behandlingType = behandlingerPåId.getValue(it.behandlingId),
                              vedtakstidspunkt = it.vedtakstidspunkt,
                              saksbehandler = it.saksbehandler,
                              andel = it.andel.tilDto(),
                              aktivitet = it.vedtaksperiode.aktivitet,
                              periodeType = it.vedtaksperiode.periodeType,
                              endring = it.endring)
        }
    }

    private fun lagHistorikkHolders(tilkjentYtelser: List<TilkjentYtelse>,
                                    vedtaksliste: List<Vedtak>): List<AndelHistorikkHolder> {
        val historikk = mutableListOf<AndelHistorikkHolder>()

        val vedtaksperioderPåBehandling = vedtaksliste.associate {
            it.behandlingId to (it.perioder?.perioder ?: error("Finner ikke vedtaksperioder på behandling=${it.behandlingId}"))
        }

        tilkjentYtelser.forEach { tilkjentYtelse ->
            val vedtaksperioder = vedtaksperioderPåBehandling.getValue(tilkjentYtelse.behandlingId)

            tilkjentYtelse.andelerTilkjentYtelse.forEach { andel ->
                val andelFraHistorikk = finnTilsvarendeAndelIHistorikk(historikk, andel)
                val vedtaksperiode = finnVedtaksperiodeForAndel(andel, vedtaksperioder)
                if (andelFraHistorikk == null) {
                    val index = finnIndeksForNyAndel(historikk, andel)
                    historikk.add(index, lagNyAndel(tilkjentYtelse, andel, vedtaksperiode))
                } else {
                    val endringType = andelFraHistorikk.finnEndringstype(andel, vedtaksperiode)
                    if (endringType != null) {
                        andelFraHistorikk.andel = andel
                        andelFraHistorikk.endring = lagEndring(endringType, tilkjentYtelse)
                        andelFraHistorikk.vedtaksperiode = vedtaksperiode
                    }
                    andelFraHistorikk.kontrollert = tilkjentYtelse.id
                }
            }

            markerAndelerSomErFjernet(historikk, tilkjentYtelse)
        }
        return historikk
    }

    private fun finnVedtaksperiodeForAndel(andel: AndelTilkjentYtelse, vedtaksperioder: List<Vedtaksperiode>): Vedtaksperiode {
        return vedtaksperioder.first { andel.stønadFom in it.datoFra..it.datoTil }
    }

    private fun sorterTilkjentYtelser(tilkjentYtelser: List<TilkjentYtelse>): List<TilkjentYtelse> =
            tilkjentYtelser.sortedBy { it.sporbar.opprettetTid }
                    .map { it.copy(andelerTilkjentYtelse = it.andelerTilkjentYtelse.sortedBy(AndelTilkjentYtelse::stønadFom)) }

    private fun lagNyAndel(tilkjentYtelse: TilkjentYtelse,
                           andel: AndelTilkjentYtelse,
                           vedtaksperiode: Vedtaksperiode) =
            AndelHistorikkHolder(behandlingId = tilkjentYtelse.behandlingId,
                                 vedtakstidspunkt = tilkjentYtelse.vedtakstidspunkt,
                                 saksbehandler = tilkjentYtelse.sporbar.opprettetAv,
                                 andel = andel,
                                 endring = null,
                                 vedtaksperiode = vedtaksperiode,
                                 kontrollert = tilkjentYtelse.id)

    private fun AndelHistorikkHolder.finnEndringstype(andel: AndelTilkjentYtelse, vedtaksperiode: Vedtaksperiode): EndringType? {
        return when {
            aktivitetEllerPeriodeTypeHarEndretSeg(vedtaksperiode) -> EndringType.ENDRET
            this.andel.stønadTom != andel.stønadTom || this.andel.beløp != andel.beløp -> EndringType.ENDRET
            this.andel.inntekt != andel.inntekt -> EndringType.ENDRING_I_INNTEKT
            else -> null
        }
    }

    private fun AndelHistorikkHolder.aktivitetEllerPeriodeTypeHarEndretSeg(vedtaksperiode: Vedtaksperiode) =
            this.vedtaksperiode.aktivitet != vedtaksperiode.aktivitet ||
            this.vedtaksperiode.periodeType != vedtaksperiode.periodeType

    private fun lagEndring(type: EndringType, tilkjentYtelse: TilkjentYtelse) =
            HistorikkEndring(type = type,
                             behandlingId = tilkjentYtelse.behandlingId,
                             vedtakstidspunkt = tilkjentYtelse.vedtakstidspunkt)

    /**
     * Finner indeks for andelen etter [andel], hvis den ikke finnes returneres [historikk] sin size
     */
    private fun finnIndeksForNyAndel(historikk: List<AndelHistorikkHolder>,
                                     andel: AndelTilkjentYtelse): Int {
        val index = historikk.indexOfFirst { it.andel.stønadFom.isAfter(andel.stønadTom) }
        return if (index == -1) historikk.size else index
    }

    private fun finnTilsvarendeAndelIHistorikk(historikk: List<AndelHistorikkHolder>,
                                               andel: AndelTilkjentYtelse): AndelHistorikkHolder? =
            historikk.findLast { it.endring?.type != EndringType.FJERNET && it.andel.stønadFom == andel.stønadFom }

    /**
     * Hvis en [tilkjentYtelse] sin behandlingId ikke er lik andelene i historikk sine verdier for kontrollert,
     * så betyr det att selve andelen i historikken er fjernet då den ikke har blitt kontrollert i denne iterasjonen.
     * Den markeres då som fjernet.
     */
    private fun markerAndelerSomErFjernet(historikk: MutableList<AndelHistorikkHolder>,
                                          tilkjentYtelse: TilkjentYtelse) {
        historikk.filterNot { erAlleredeFjernetEllerKontrollert(it, tilkjentYtelse) }.forEach {
            it.endring = lagEndring(EndringType.FJERNET, tilkjentYtelse)
        }
    }

    private fun erAlleredeFjernetEllerKontrollert(historikk: AndelHistorikkHolder,
                                                  tilkjentYtelse: TilkjentYtelse) =
            historikk.endring?.type == EndringType.FJERNET || historikk.kontrollert == tilkjentYtelse.id
}
