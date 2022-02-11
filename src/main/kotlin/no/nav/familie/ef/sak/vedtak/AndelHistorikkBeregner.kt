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
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import java.time.LocalDateTime
import java.util.UUID

enum class EndringType {
    FJERNET,
    ERSTATTET,
    SPLITTET,
}

data class AndelHistorikkDto(val behandlingId: UUID,
                             val behandlingType: BehandlingType,
                             val vedtakstidspunkt: LocalDateTime,
                             val saksbehandler: String,
                             val andel: AndelTilkjentYtelseDto,
                             val aktivitet: AktivitetType,
                             val periodeType: VedtaksperiodeType,
                             val endring: HistorikkEndring?,
                             val sanksjonsårsak: Sanksjonsårsak? = null)

data class HistorikkEndring(val type: EndringType,
                            val behandlingId: UUID,
                            val vedtakstidspunkt: LocalDateTime)

object AndelHistorikkBeregner {

    /**
     * @param kontrollert brukes for å sjekke om en andel er fjernet eller ikke
     */
    private data class AndelHistorikkHolder(
            val behandlingId: UUID,
            val vedtakstidspunkt: LocalDateTime,
            val saksbehandler: String,
            var andel: AndelTilkjentYtelse,
            var endring: HistorikkEndring?,
            var vedtaksperiode: Vedtaksperiode,
            var kontrollert: UUID
    )

    fun lagHistorikk(tilkjentYtelser: List<TilkjentYtelse>,
                     vedtaksliste: List<Vedtak>,
                     behandlinger: List<Behandling>,
                     tilOgMedBehandlingId: UUID?): List<AndelHistorikkDto> {
        return if (tilOgMedBehandlingId == null) {
            lagHistorikk(tilkjentYtelser, vedtaksliste, behandlinger)
        } else {
            lagHistorikkTilBehandlingId(tilkjentYtelser, vedtaksliste, behandlinger, tilOgMedBehandlingId)
        }
    }

    /**
     * Filtrerer vekk data som kommer etter behandlingen som man sender inn
     */
    private fun lagHistorikkTilBehandlingId(tilkjentYtelser: List<TilkjentYtelse>,
                                            vedtaksliste: List<Vedtak>,
                                            behandlinger: List<Behandling>,
                                            tilOgMedBehandlingId: UUID?): List<AndelHistorikkDto> {
        val filtrerteBehandlinger = filtrerBehandlinger(behandlinger, tilOgMedBehandlingId)

        val filtrerteBehandlingId = filtrerteBehandlinger.map { it.id }.toSet()
        val filtrerteVedtak = vedtaksliste.filter { filtrerteBehandlingId.contains(it.behandlingId) }
        val filtrerteTilkjentYtelse = tilkjentYtelser.filter { filtrerteBehandlingId.contains(it.behandlingId) }

        return lagHistorikk(filtrerteTilkjentYtelse, filtrerteVedtak, filtrerteBehandlinger)
    }

    private fun filtrerBehandlinger(behandlinger: List<Behandling>,
                                    tilOgMedBehandlingId: UUID?): List<Behandling> {
        val tilOgMedBehandling = behandlinger.firstOrNull { it.id == tilOgMedBehandlingId }
                                 ?: error("Finner ikke behandling $tilOgMedBehandlingId")
        return tilOgMedBehandling.let { tomBehandling ->
            behandlinger.filter { it.sporbar.opprettetTid < tomBehandling.sporbar.opprettetTid } + tomBehandling
        }
    }

    private fun lagHistorikk(tilkjentYtelser: List<TilkjentYtelse>,
                             vedtaksliste: List<Vedtak>,
                             behandlinger: List<Behandling>): List<AndelHistorikkDto> {
        val (sanksjonsvedtak, vedtakMinusSanksjon) = vedtaksliste.partition { it.resultatType == ResultatType.SANKSJONERE }
        val (ytelserForSanksjonsvedtak, ytelserForVedtakMinusSanksjon) = tilkjentYtelser.partition {
            it.behandlingId in sanksjonsvedtak.map { it.behandlingId }
                    .toSet()
        }

        val behandlingerPåId = behandlinger.associate { it.id to it }
        val vedtakshistorikkForSanksjon = lagHistorikkForSanksjonsvedtak(sanksjonsvedtak, ytelserForSanksjonsvedtak, behandlingerPåId)

        val historikkHoldersForVedtakMinusSanksjon = lagHistorikkHolders(ytelserForVedtakMinusSanksjon, vedtakMinusSanksjon)
        val vedtakshistorikkMinusSanksjon = historikkHoldersForVedtakMinusSanksjon.map {
            AndelHistorikkDto(behandlingId = it.behandlingId,
                              behandlingType = behandlingerPåId.getValue(it.behandlingId).type,
                              vedtakstidspunkt = it.vedtakstidspunkt,
                              saksbehandler = it.saksbehandler,
                              andel = it.andel.tilDto(),
                              aktivitet = it.vedtaksperiode.aktivitet,
                              periodeType = it.vedtaksperiode.periodeType,
                              endring = it.endring)
        }

        return sorterVedtaksHistorikk(vedtakshistorikkMinusSanksjon + vedtakshistorikkForSanksjon);
    }

    private fun lagHistorikkHolders(tilkjentYtelser: List<TilkjentYtelse>,
                                    vedtaksliste: List<Vedtak>): List<AndelHistorikkHolder> {
        val historikk = mutableListOf<AndelHistorikkHolder>()

        val vedtaksperioderPåBehandling = lagVedtaksperioderPerBehandling(vedtaksliste, tilkjentYtelser)

        tilkjentYtelser.forEach { tilkjentYtelse ->
            val vedtaksperioder = vedtaksperioderPåBehandling.getValue(tilkjentYtelse.behandlingId)

            tilkjentYtelse.andelerTilkjentYtelse.forEach { andel ->
                val andelFraHistorikk = finnTilsvarendeAndelIHistorikk(historikk, andel)
                val index = finnIndeksForNyAndel(historikk, andel)
                val vedtaksperiode = finnVedtaksperiodeForAndel(andel, vedtaksperioder)
                if (andelFraHistorikk == null) {
                    historikk.add(index, lagNyAndel(tilkjentYtelse, andel, vedtaksperiode))
                } else {
                    markerTidligereMedEndringOgReturnerNyAndel(tilkjentYtelse, andel, andelFraHistorikk, vedtaksperiode)
                            ?.let { historikk.add(index, it) }
                }
            }

            markerAndelerSomErFjernet(historikk, tilkjentYtelse)
        }
        return historikk
    }

    /**
     * Markerer endrede med riktig type endret
     * Splitter eventuellt opp perioder som blivit avkortet, eks der stønadsbeløpet endret seg fra gitt måned
     */
    private fun markerTidligereMedEndringOgReturnerNyAndel(
            tilkjentYtelse: TilkjentYtelse,
            andel: AndelTilkjentYtelse,
            andelFraHistorikk: AndelHistorikkHolder,
            vedtaksperiode: Vedtaksperiode
    ): AndelHistorikkHolder? {
        // settes for å senere markere de som fjernet hvis de blir markert som endret
        andelFraHistorikk.kontrollert = tilkjentYtelse.id

        return andelFraHistorikk.finnEndringstype(andel, vedtaksperiode)?.let { endringType ->
            val andelHistorikk = andelFraHistorikk.andel
            andelFraHistorikk.endring = lagEndring(endringType, tilkjentYtelse)

            return if (endringType == EndringType.SPLITTET) {
                andelFraHistorikk.andel = andelHistorikk.copy(stønadTom = andel.stønadTom)
                andelFraHistorikk.copy(andel = andelHistorikk.copy(stønadFom = andel.stønadTom.plusDays(1)),
                                       endring = lagEndring(EndringType.FJERNET, tilkjentYtelse))
            } else {
                lagNyAndel(tilkjentYtelse, andel, vedtaksperiode)
            }
        }
    }

    // Pga vedtak ikke har dato for når det ble opprettet så matcher vi de sammen med tilkjent ytelse sin opprettetTid
    private fun lagVedtaksperioderPerBehandling(vedtaksliste: List<Vedtak>,
                                                tilkjentYtelser: List<TilkjentYtelse>): Map<UUID, List<Vedtaksperiode>> {
        val datoPerBehandling = tilkjentYtelser.associate { it.behandlingId to it.vedtakstidspunkt }
        return VedtakHistorikkBeregner.lagVedtaksperioderPerBehandling(vedtaksliste, datoPerBehandling)
    }

    private fun finnVedtaksperiodeForAndel(andel: AndelTilkjentYtelse, vedtaksperioder: List<Vedtaksperiode>): Vedtaksperiode {
        return vedtaksperioder.first { andel.stønadFom in it.datoFra..it.datoTil }
    }

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

    private fun AndelHistorikkHolder.finnEndringstype(tidligereAndel: AndelTilkjentYtelse,
                                                      vedtaksperiode: Vedtaksperiode): EndringType? {
        return when {
            aktivitetEllerPeriodeTypeHarEndretSeg(vedtaksperiode) -> EndringType.ERSTATTET
            this.andel.beløp != tidligereAndel.beløp -> EndringType.ERSTATTET
            this.andel.inntekt != tidligereAndel.inntekt -> EndringType.ERSTATTET
            this.andel.stønadTom < tidligereAndel.stønadTom -> EndringType.ERSTATTET
            this.andel.stønadTom > tidligereAndel.stønadTom -> EndringType.SPLITTET
            this.andel.kildeBehandlingId != tidligereAndel.kildeBehandlingId -> EndringType.FJERNET
            else -> null // Uendret
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
            historikk.findLast {
                it.endring?.type != EndringType.FJERNET &&
                it.endring?.type != EndringType.ERSTATTET &&
                it.andel.stønadFom == andel.stønadFom
            }

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
            historikk.endring?.type == EndringType.FJERNET ||
            historikk.endring?.type == EndringType.ERSTATTET ||
            historikk.kontrollert == tilkjentYtelse.id

    private fun lagHistorikkForSanksjonsvedtak(sanksjonsvedtak: List<Vedtak>,
                                               tilkjentYtelserForSanksjon: List<TilkjentYtelse>,
                                               behandlingerPåId: Map<UUID, Behandling>): List<AndelHistorikkDto> {
        val sanksjonsvedtakPåId = sanksjonsvedtak.associate { it.behandlingId to it }

        return tilkjentYtelserForSanksjon.map {
            AndelHistorikkDto(behandlingId = it.behandlingId,
                              behandlingType = behandlingerPåId.getValue(it.behandlingId).type,
                              vedtakstidspunkt = it.vedtakstidspunkt,
                              saksbehandler = it.sporbar.opprettetAv,
                              andel = it.andelerTilkjentYtelse.first().tilDto(),
                              aktivitet = sanksjonsvedtakPåId.getValue(it.behandlingId).perioder?.perioder?.first()?.aktivitet
                                          ?: error("Finner ikke aktivitet for sanksjonert vedtak=${it.behandlingId}"),
                              periodeType = sanksjonsvedtakPåId.getValue(it.behandlingId).perioder?.perioder?.first()?.periodeType
                                            ?: error("Finner ikke periodetype for sanksjonert vedtak=${it.behandlingId}"),
                              endring = null,
                              sanksjonsårsak = sanksjonsvedtakPåId.get(it.behandlingId)?.sanksjonsårsak
                                               ?: error("Finner ikke sanksjonsårsak for sanksjonert vedtak=${it.behandlingId}"))
        }
    }

    private fun sorterVedtaksHistorikk(historikk: List<AndelHistorikkDto>): List<AndelHistorikkDto> = historikk.sortedBy { it.andel.stønadFra }

}
