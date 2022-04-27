package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.historikk.BehandlingHistorikkUtil.lagBehandlingHistorikkData
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkBeregner.lagVedtaksperioderPerBehandling
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import java.time.LocalDateTime
import java.util.UUID

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
            var vedtaksperiode: Vedtakshistorikkperiode,
            var kontrollert: UUID
    )

    fun lagHistorikk(tilkjentYtelser: List<TilkjentYtelse>,
                     vedtaksliste: List<Vedtak>,
                     behandlinger: List<Behandling>,
                     tilOgMedBehandlingId: UUID?,
                     behandlingIdsTilAktivitetArbeid: Map<UUID, SvarId?>): List<AndelHistorikkDto> {
        val behandlingHistorikkData = lagBehandlingHistorikkData(vedtaksliste, tilkjentYtelser, behandlingIdsTilAktivitetArbeid)
        return if (tilOgMedBehandlingId == null) {
            lagHistorikk(tilkjentYtelser, behandlingHistorikkData, behandlinger)
        } else {
            lagHistorikkTilBehandlingId(tilkjentYtelser, behandlingHistorikkData, behandlinger, tilOgMedBehandlingId)
        }
    }

    /**
     * Filtrerer vekk data som kommer etter behandlingen som man sender inn
     */
    private fun lagHistorikkTilBehandlingId(tilkjentYtelser: List<TilkjentYtelse>,
                                            vedtaksliste: List<BehandlingHistorikkData>,
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
                             behandlingHistorikkData: List<BehandlingHistorikkData>,
                             behandlinger: List<Behandling>): List<AndelHistorikkDto> {
        val historikk = lagHistorikkHolders(sorterTilkjentYtelser(tilkjentYtelser), behandlingHistorikkData)
        val behandlingerPåId = behandlinger.associate { it.id to it.type }

        return historikk.map {
            val vedtaksperiode = it.vedtaksperiode
            val aktivitet = if (vedtaksperiode is VedtakshistorikkperiodeOvergangsstønad) vedtaksperiode.aktivitet else null
            val periodeType = if (vedtaksperiode is VedtakshistorikkperiodeOvergangsstønad) vedtaksperiode.periodeType else null
            val barnetilsyn = if (vedtaksperiode is VedtakshistorikkperiodeBarnetilsyn) vedtaksperiode else null

            AndelHistorikkDto(behandlingId = it.behandlingId,
                              behandlingType = behandlingerPåId.getValue(it.behandlingId),
                              vedtakstidspunkt = it.vedtakstidspunkt,
                              saksbehandler = it.saksbehandler,
                              andel = AndelMedGrunnlagDto(andel = it.andel, barnetilsyn),
                              aktivitet = aktivitet,
                              periodeType = periodeType,
                              endring = it.endring,
                              aktivitetArbeid = barnetilsyn?.aktivitetArbeid,
                              erSanksjon = vedtaksperiode.erSanksjon)
        }
    }

    private fun lagHistorikkHolders(tilkjentYtelser: List<TilkjentYtelse>,
                                    behandlingHistorikkData: List<BehandlingHistorikkData>): List<AndelHistorikkHolder> {
        val historikk = mutableListOf<AndelHistorikkHolder>()

        val vedtaksperioderPåBehandling = lagVedtaksperioderPerBehandling(behandlingHistorikkData)

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

    private fun lagAndelerFraSanksjoner(vedtaksperioder: List<Vedtakshistorikkperiode>,
                                        tilkjentYtelse: TilkjentYtelse) =
            vedtaksperioder.filter { it.erSanksjon }.map {
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
            vedtaksperiode: Vedtakshistorikkperiode
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

    private fun finnVedtaksperiodeForAndel(andel: AndelTilkjentYtelse,
                                           vedtaksperioder: List<Vedtakshistorikkperiode>): Vedtakshistorikkperiode {
        return vedtaksperioder.first { andel.stønadFom in it.datoFra..it.datoTil }
    }

    private fun sorterTilkjentYtelser(tilkjentYtelser: List<TilkjentYtelse>): List<TilkjentYtelse> =
            tilkjentYtelser.sortedBy { it.sporbar.opprettetTid }
                    .map { it.copy(andelerTilkjentYtelse = it.andelerTilkjentYtelse.sortedBy(AndelTilkjentYtelse::stønadFom)) }

    private fun lagNyAndel(tilkjentYtelse: TilkjentYtelse,
                           andel: AndelTilkjentYtelse,
                           vedtaksperiode: Vedtakshistorikkperiode) =
            AndelHistorikkHolder(behandlingId = tilkjentYtelse.behandlingId,
                                 vedtakstidspunkt = tilkjentYtelse.vedtakstidspunkt,
                                 saksbehandler = tilkjentYtelse.sporbar.opprettetAv,
                                 andel = andel,
                                 endring = null,
                                 vedtaksperiode = vedtaksperiode,
                                 kontrollert = tilkjentYtelse.id)

    private fun AndelHistorikkHolder.finnEndringstype(tidligereAndel: AndelTilkjentYtelse,
                                                      tidligerePeriode: Vedtakshistorikkperiode): EndringType? {
        return when {
            erSanksjonMedSammePerioder(tidligereAndel, tidligerePeriode) -> null
            aktivitetEllerPeriodeTypeHarEndretSeg(tidligerePeriode) -> EndringType.ERSTATTET
            this.andel.beløp != tidligereAndel.beløp -> EndringType.ERSTATTET
            this.andel.inntekt != tidligereAndel.inntekt -> EndringType.ERSTATTET
            erEndringerForBarnetilsyn(this.vedtaksperiode, tidligerePeriode) -> EndringType.ERSTATTET
            this.andel.stønadTom < tidligereAndel.stønadTom -> EndringType.ERSTATTET
            this.andel.stønadTom > tidligereAndel.stønadTom -> EndringType.SPLITTET
            this.andel.kildeBehandlingId != tidligereAndel.kildeBehandlingId -> EndringType.FJERNET
            else -> null // Uendret
        }
    }

    private fun erEndringerForBarnetilsyn(first: Vedtakshistorikkperiode,
                                          second: Vedtakshistorikkperiode): Boolean {
        if (first !is VedtakshistorikkperiodeBarnetilsyn ||
            second !is VedtakshistorikkperiodeBarnetilsyn) {
            return false
        }
        return first.antallBarn != second.antallBarn ||
               first.utgifter != second.utgifter ||
               first.kontantstøtte != second.kontantstøtte ||
               first.tilleggsstønad != second.tilleggsstønad
    }

    private fun AndelHistorikkHolder.erSanksjonMedSammePerioder(tidligereAndel: AndelTilkjentYtelse,
                                                                tidligerePeriode: Vedtakshistorikkperiode): Boolean {
        val vedtaksperiode = this.vedtaksperiode
        if (vedtaksperiode !is VedtakshistorikkperiodeOvergangsstønad ||
            tidligerePeriode !is VedtakshistorikkperiodeOvergangsstønad) {
            return false
        }
        return vedtaksperiode.erSanksjon && tidligerePeriode.erSanksjon
               && vedtaksperiode.datoFra == tidligereAndel.stønadFom && vedtaksperiode.datoTil == tidligereAndel.stønadTom
    }

    private fun AndelHistorikkHolder.aktivitetEllerPeriodeTypeHarEndretSeg(annenVedtaksperiode: Vedtakshistorikkperiode): Boolean {
        val vedtaksperiode = this.vedtaksperiode
        if (vedtaksperiode.javaClass != annenVedtaksperiode.javaClass) {
            return true
        }
        //Kun for typkasting
        if (vedtaksperiode !is VedtakshistorikkperiodeOvergangsstønad ||
            annenVedtaksperiode !is VedtakshistorikkperiodeOvergangsstønad) {
            return false
        }
        return vedtaksperiode.aktivitet != annenVedtaksperiode.aktivitet ||
               vedtaksperiode.periodeType != annenVedtaksperiode.periodeType
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
