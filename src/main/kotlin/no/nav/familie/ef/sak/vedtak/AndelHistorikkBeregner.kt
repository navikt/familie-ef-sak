package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import java.time.LocalDate
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
                             val andel: AndelDto,
                             val aktivitet: AktivitetType?,
                             val periodeType: VedtaksperiodeType?,
                             val endring: HistorikkEndring?)

// TODO Trenger ny type i frontend som ikke har inntekt
data class AndelDto(
        val beløp: Int,
        val stønadFra: LocalDate,
        val stønadTil: LocalDate,
        val inntekt: Int,
        val inntektsreduksjon: Int,
        val samordningsfradrag: Int,
        val kontantstøtte: Int = 0,
        val tillegsstønad: Int = 0
) {

    constructor(andel: AndelTilkjentYtelse, kontantstøtte: Int, tillegsstønad: Int) : this(
            beløp = andel.beløp,
            stønadFra = andel.stønadFom,
            stønadTil = andel.stønadTom,
            inntekt = andel.inntekt,
            inntektsreduksjon = andel.inntektsreduksjon,
            samordningsfradrag = andel.samordningsfradrag,
            kontantstøtte = kontantstøtte,
            tillegsstønad = tillegsstønad
    )
}

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
                     tilOgMedBehandlingId: UUID?): List<AndelHistorikkDto> {
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
                             vedtaksliste: List<BehandlingVedtakDto>,
                             behandlinger: List<Behandling>): List<AndelHistorikkDto> {
        val historikk = lagHistorikkHolders(sorterTilkjentYtelser(tilkjentYtelser), vedtaksliste)
        val behandlingerPåId = behandlinger.associate { it.id to it.type }

        return historikk.mapNotNull {
            val vedtaksperiode = it.vedtaksperiode
            if (vedtaksperiode !is VedtakHistorikkBeregner.VedtaksinformasjonOvergangsstønad) {
                return@mapNotNull null
            }
            AndelHistorikkDto(behandlingId = it.behandlingId,
                              behandlingType = behandlingerPåId.getValue(it.behandlingId),
                              vedtakstidspunkt = it.vedtakstidspunkt,
                              saksbehandler = it.saksbehandler,
                              andel = AndelDto(andel = it.andel,
                                               kontantstøtte = 0,
                                               tillegsstønad = 0
                              ),
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
                if (it !is VedtakHistorikkBeregner.VedtaksinformasjonOvergangsstønad) {
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
                                                                vedtaksperiode: VedtakHistorikkBeregner.Vedtaksinformasjon): Boolean {
        val thisVedtaksperiode = this.vedtaksperiode
        return (thisVedtaksperiode is VedtakHistorikkBeregner.VedtaksinformasjonOvergangsstønad && vedtaksperiode is VedtakHistorikkBeregner.VedtaksinformasjonOvergangsstønad &&
                thisVedtaksperiode.periodeType == VedtaksperiodeType.SANKSJON && vedtaksperiode.periodeType == VedtaksperiodeType.SANKSJON
                && thisVedtaksperiode.datoFra == tidligereAndel.stønadFom && thisVedtaksperiode.datoTil == tidligereAndel.stønadTom)
    }


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
