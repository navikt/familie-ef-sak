package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.tilkjentytelse.AndelTilkjentYtelseDto
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.tilDto
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import java.time.LocalDateTime
import java.util.UUID

enum class EndringType {
    FJERNET,
    ERSTATTET,
    SPLITTET,
}

sealed class PeriodeHistorikkDto {

    abstract val behandlingId: UUID
    abstract val behandlingType: BehandlingType
    abstract val vedtakstidspunkt: LocalDateTime
    abstract val saksbehandler: String
    abstract val andel: AndelTilkjentYtelseDto
    abstract val endring: HistorikkEndring?
}

data class PeriodeHistorikkSanksjon(
        override val behandlingId: UUID,
        override val behandlingType: BehandlingType,
        override val vedtakstidspunkt: LocalDateTime,
        override val saksbehandler: String,
        override val andel: AndelTilkjentYtelseDto,
        override val endring: HistorikkEndring?,
) : PeriodeHistorikkDto() {

    val erSanksjon = true
}

data class PeriodeHistorikkOvergangsstønadDto(
        override val behandlingId: UUID,
        override val behandlingType: BehandlingType,
        override val vedtakstidspunkt: LocalDateTime,
        override val saksbehandler: String,
        override val andel: AndelTilkjentYtelseDto,
        override val endring: HistorikkEndring?,

        val aktivitet: AktivitetType,
        val periodeType: VedtaksperiodeType,
) : PeriodeHistorikkDto()

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
            var vedtaksperiode: VedtakHistorikkBeregner.Vedtaksinformasjon,
            var kontrollert: UUID
    )

    fun lagHistorikk(tilkjentYtelser: List<TilkjentYtelse>,
                     vedtaksliste: List<Vedtak>,
                     behandlinger: List<Behandling>,
                     tilOgMedBehandlingId: UUID?): List<PeriodeHistorikkDto> {
        val behandlingVedtakDto = vedtaksliste.map { BehandlingVedtakDto(it.behandlingId, it.tilVedtakDto()) }
        return if (tilOgMedBehandlingId == null) {
            lagHistorikk(tilkjentYtelser, behandlingVedtakDto, behandlinger)
        } else {
            lagHistorikkTilBehandlingId(tilkjentYtelser, behandlingVedtakDto, behandlinger, tilOgMedBehandlingId)
        }
    }

    class BehandlingVedtakDto(
            val behandlingId: UUID,
            val vedtakDto: VedtakDto
    )

    /**
     * Filtrerer vekk data som kommer etter behandlingen som man sender inn
     */
    private fun lagHistorikkTilBehandlingId(tilkjentYtelser: List<TilkjentYtelse>,
                                            vedtaksliste: List<BehandlingVedtakDto>,
                                            behandlinger: List<Behandling>,
                                            tilOgMedBehandlingId: UUID?): List<PeriodeHistorikkDto> {
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
                             vedtaksliste: List<BehandlingVedtakDto>,
                             behandlinger: List<Behandling>): List<PeriodeHistorikkDto> {
        val historikk = lagHistorikkHolders(sorterTilkjentYtelser(tilkjentYtelser), vedtaksliste)
        val behandlingerPåId = behandlinger.associate { it.id to it.type }

        return historikk.mapNotNull {
            val vedtaksperiode = it.vedtaksperiode
            if (vedtaksperiode !is VedtakHistorikkBeregner.VedtaksinformasjonOvergangsstønad) {
                return@mapNotNull null
            }
            PeriodeHistorikkOvergangsstønadDto(behandlingId = it.behandlingId,
                                               behandlingType = behandlingerPåId.getValue(it.behandlingId),
                                               vedtakstidspunkt = it.vedtakstidspunkt,
                                               saksbehandler = it.saksbehandler,
                                               andel = it.andel.tilDto(),
                                               aktivitet = vedtaksperiode.aktivitet,
                                               periodeType = vedtaksperiode.periodeType,
                                               endring = it.endring)
        }
    }

    private fun lagHistorikkHolders(tilkjentYtelser: List<TilkjentYtelse>,
                                    vedtaksliste: List<BehandlingVedtakDto>): List<AndelHistorikkHolder> {
        val historikk = mutableListOf<AndelHistorikkHolder>()

        val vedtaksperioderPåBehandling = lagVedtaksperioderPerBehandling(vedtaksliste, tilkjentYtelser)

        tilkjentYtelser.forEach { tilkjentYtelse ->
            val vedtaksperioder = vedtaksperioderPåBehandling.getValue(tilkjentYtelse.behandlingId)
            val andelerFraSanksjon = lagAndelerFraSanksjoner(vedtaksperioder, tilkjentYtelse)
            (tilkjentYtelse.andelerTilkjentYtelse + andelerFraSanksjon).forEach { andel ->
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

    private fun lagAndelerFraSanksjoner(vedtaksperioder: List<VedtakHistorikkBeregner.Vedtaksinformasjon>,
                                        tilkjentYtelse: TilkjentYtelse) =
            vedtaksperioder.mapNotNull {
                if (it !is VedtakHistorikkBeregner.VedtaksinformasjonSanksjon) {
                    return@mapNotNull null
                }
                AndelTilkjentYtelse(beløp = 0,
                                    stønadFom = it.datoFra,
                                    stønadTom = it.datoTil,
                                    "",
                                    0,
                                    0,
                                    0,
                                    tilkjentYtelse.behandlingId)
            }

    /**
     * Markerer endrede med riktig type endret
     * Splitter eventuellt opp perioder som blivit avkortet, eks der stønadsbeløpet endret seg fra gitt måned
     */
    private fun markerTidligereMedEndringOgReturnerNyAndel(
            tilkjentYtelse: TilkjentYtelse,
            andel: AndelTilkjentYtelse,
            andelFraHistorikk: AndelHistorikkHolder,
            vedtaksperiode: VedtakHistorikkBeregner.Vedtaksinformasjon
    ): AndelHistorikkHolder? {
        // settes for å senere markere de som fjernet hvis de blir markert som endret
        andelFraHistorikk.kontrollert = tilkjentYtelse.id

        return andelFraHistorikk.finnEndringstype(andel, vedtaksperiode)?.let { endringType ->
            val andelHistorikk = andelFraHistorikk.andel
            andelFraHistorikk.endring = lagEndring(endringType, tilkjentYtelse)

            return if (endringType == EndringType.SPLITTET) {
                andelFraHistorikk.andel = andelHistorikk.copy(stønadTom = andel.stønadTom,
                                                              kildeBehandlingId = andel.kildeBehandlingId)
                andelFraHistorikk.copy(andel = andelHistorikk.copy(stønadFom = andel.stønadTom.plusDays(1)),
                                       endring = lagEndring(EndringType.FJERNET, tilkjentYtelse))
            } else {
                lagNyAndel(tilkjentYtelse, andel, vedtaksperiode)
            }
        }
    }

    // Pga vedtak ikke har dato for når det ble opprettet så matcher vi de sammen med tilkjent ytelse sin opprettetTid
    private fun lagVedtaksperioderPerBehandling(vedtaksliste: List<BehandlingVedtakDto>,
                                                tilkjentYtelser: List<TilkjentYtelse>): Map<UUID, List<VedtakHistorikkBeregner.Vedtaksinformasjon>> {
        val datoPerBehandling = tilkjentYtelser.associate { it.behandlingId to it.vedtakstidspunkt }
        return VedtakHistorikkBeregner.lagVedtaksperioderPerBehandling(vedtaksliste, datoPerBehandling)
    }

    private fun finnVedtaksperiodeForAndel(andel: AndelTilkjentYtelse,
                                           vedtaksperioder: List<VedtakHistorikkBeregner.Vedtaksinformasjon>): VedtakHistorikkBeregner.Vedtaksinformasjon {
        return vedtaksperioder.first { andel.stønadFom in it.datoFra..it.datoTil }
    }

    private fun sorterTilkjentYtelser(tilkjentYtelser: List<TilkjentYtelse>): List<TilkjentYtelse> =
            tilkjentYtelser.sortedBy { it.sporbar.opprettetTid }
                    .map { it.copy(andelerTilkjentYtelse = it.andelerTilkjentYtelse.sortedBy(AndelTilkjentYtelse::stønadFom)) }

    private fun lagNyAndel(tilkjentYtelse: TilkjentYtelse,
                           andel: AndelTilkjentYtelse,
                           vedtaksperiode: VedtakHistorikkBeregner.Vedtaksinformasjon) =
            AndelHistorikkHolder(behandlingId = tilkjentYtelse.behandlingId,
                                 vedtakstidspunkt = tilkjentYtelse.vedtakstidspunkt,
                                 saksbehandler = tilkjentYtelse.sporbar.opprettetAv,
                                 andel = andel,
                                 endring = null,
                                 vedtaksperiode = vedtaksperiode,
                                 kontrollert = tilkjentYtelse.id)

    private fun AndelHistorikkHolder.finnEndringstype(tidligereAndel: AndelTilkjentYtelse,
                                                      vedtaksperiode: VedtakHistorikkBeregner.Vedtaksinformasjon): EndringType? {
        return when {
            erSanksjonMedSammePerioder(tidligereAndel, vedtaksperiode) -> null
            aktivitetEllerPeriodeTypeHarEndretSeg(vedtaksperiode) -> EndringType.ERSTATTET
            this.andel.beløp != tidligereAndel.beløp -> EndringType.ERSTATTET
            this.andel.inntekt != tidligereAndel.inntekt -> EndringType.ERSTATTET
            this.andel.stønadTom < tidligereAndel.stønadTom -> EndringType.ERSTATTET
            this.andel.stønadTom > tidligereAndel.stønadTom -> EndringType.SPLITTET
            this.andel.kildeBehandlingId != tidligereAndel.kildeBehandlingId -> EndringType.FJERNET
            else -> null // Uendret
        }
    }

    private fun AndelHistorikkHolder.erSanksjonMedSammePerioder(tidligereAndel: AndelTilkjentYtelse,
                                                                vedtaksperiode: VedtakHistorikkBeregner.Vedtaksinformasjon) =
            this.vedtaksperiode is VedtakHistorikkBeregner.VedtaksinformasjonSanksjon && vedtaksperiode is VedtakHistorikkBeregner.VedtaksinformasjonSanksjon
            && this.vedtaksperiode.datoFra == tidligereAndel.stønadFom && this.vedtaksperiode.datoTil == tidligereAndel.stønadTom


    private fun AndelHistorikkHolder.aktivitetEllerPeriodeTypeHarEndretSeg(vedtaksperiode: VedtakHistorikkBeregner.Vedtaksinformasjon): Boolean {
        val thisVedtaksperiode = this.vedtaksperiode
        return thisVedtaksperiode.javaClass != vedtaksperiode.javaClass ||
               (thisVedtaksperiode is VedtakHistorikkBeregner.VedtaksinformasjonOvergangsstønad &&
                vedtaksperiode is VedtakHistorikkBeregner.VedtaksinformasjonOvergangsstønad &&
                (thisVedtaksperiode.aktivitet != vedtaksperiode.aktivitet ||
                 thisVedtaksperiode.periodeType != vedtaksperiode.periodeType)
               )
    }

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
}
