package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
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

    private data class TilkjentYtelseMedVedtakstidspunkt(
        val tilkjentYtelse: TilkjentYtelse,
        val vedtakstidspunkt: LocalDateTime
    )

    fun lagHistorikk(
        tilkjentYtelser: List<TilkjentYtelse>,
        vedtaksliste: List<Vedtak>,
        behandlinger: List<Behandling>,
        tilOgMedBehandlingId: UUID?,
        behandlingIdsTilAktivitetArbeid: Map<UUID, SvarId?>,
        brukIkkeVedtatteSatser: Boolean
    ): List<AndelHistorikkDto> {
        val behandlingHistorikkData =
            lagBehandlingHistorikkData(behandlinger, vedtaksliste, tilkjentYtelser, behandlingIdsTilAktivitetArbeid)
        return if (tilOgMedBehandlingId == null) {
            lagHistorikk(tilkjentYtelser, behandlingHistorikkData, behandlinger, brukIkkeVedtatteSatser)
        } else {
            lagHistorikkTilBehandlingId(tilkjentYtelser, behandlingHistorikkData, behandlinger, tilOgMedBehandlingId, brukIkkeVedtatteSatser)
        }
    }

    /**
     * Filtrerer vekk data som kommer etter behandlingen som man sender inn
     */
    private fun lagHistorikkTilBehandlingId(
        tilkjentYtelser: List<TilkjentYtelse>,
        vedtaksliste: List<BehandlingHistorikkData>,
        behandlinger: List<Behandling>,
        tilOgMedBehandlingId: UUID?,
        brukIkkeVedtatteSatser: Boolean
    ): List<AndelHistorikkDto> {
        val filtrerteBehandlinger = filtrerBehandlinger(behandlinger, tilOgMedBehandlingId)

        val filtrerteBehandlingId = filtrerteBehandlinger.map { it.id }.toSet()
        val filtrerteVedtak = vedtaksliste.filter { filtrerteBehandlingId.contains(it.behandlingId) }
        val filtrerteTilkjentYtelse = tilkjentYtelser.filter { filtrerteBehandlingId.contains(it.behandlingId) }

        return lagHistorikk(filtrerteTilkjentYtelse, filtrerteVedtak, filtrerteBehandlinger, brukIkkeVedtatteSatser)
    }

    private fun filtrerBehandlinger(
        behandlinger: List<Behandling>,
        tilOgMedBehandlingId: UUID?
    ): List<Behandling> {
        val tilOgMedBehandling = behandlinger.firstOrNull { it.id == tilOgMedBehandlingId }
            ?: error("Finner ikke behandling $tilOgMedBehandlingId")
        return tilOgMedBehandling.let { tomBehandling ->
            behandlinger.filter { it.sporbar.opprettetTid < tomBehandling.sporbar.opprettetTid } + tomBehandling
        }
    }

    private fun lagHistorikk(
        tilkjentYtelser: List<TilkjentYtelse>,
        behandlingHistorikkData: List<BehandlingHistorikkData>,
        behandlinger: List<Behandling>,
        brukIkkeVedtatteSatser: Boolean
    ): List<AndelHistorikkDto> {
        val historikk = lagHistorikkHolders(sorterTilkjentYtelser(tilkjentYtelser), behandlingHistorikkData, brukIkkeVedtatteSatser)
        val behandlingerPåId = behandlinger.associateBy { it.id }

        return historikk.map {
            val vedtaksperiode = it.vedtaksperiode
            val aktivitet = if (vedtaksperiode is VedtakshistorikkperiodeOvergangsstønad) vedtaksperiode.aktivitet else null
            val periodeType = if (vedtaksperiode is VedtakshistorikkperiodeOvergangsstønad) vedtaksperiode.periodeType else null
            val barnetilsyn = if (vedtaksperiode is VedtakshistorikkperiodeBarnetilsyn) vedtaksperiode else null
            val behandling = behandlingerPåId.getValue(it.behandlingId)

            AndelHistorikkDto(
                behandlingId = it.behandlingId,
                behandlingType = behandling.type,
                behandlingÅrsak = behandling.årsak,
                vedtakstidspunkt = it.vedtakstidspunkt,
                saksbehandler = it.saksbehandler,
                andel = AndelMedGrunnlagDto(andel = it.andel, barnetilsyn),
                aktivitet = aktivitet,
                periodeType = periodeType,
                endring = it.endring,
                aktivitetArbeid = barnetilsyn?.aktivitetArbeid,
                erSanksjon = vedtaksperiode.erSanksjon,
                sanksjonsårsak = vedtaksperiode.sanksjonsårsak
            )
        }
    }

    private fun lagHistorikkHolders(
        tilkjentYtelser: List<TilkjentYtelse>,
        behandlingHistorikkData: List<BehandlingHistorikkData>,
        brukIkkeVedtatteSatser: Boolean
    ): List<AndelHistorikkHolder> {
        val historikk = mutableListOf<AndelHistorikkHolder>()

        val vedtaksdataPerBehandling = lagVedtaksperioderPerBehandling(behandlingHistorikkData, brukIkkeVedtatteSatser)

        tilkjentYtelser.forEach { tilkjentYtelse ->
            val vedtaksdata = vedtaksdataPerBehandling.getValue(tilkjentYtelse.behandlingId)
            val vedtaksperioder = vedtaksdata.perioder
            val tilkjentYtelseMedVedtakstidspunkt = TilkjentYtelseMedVedtakstidspunkt(tilkjentYtelse, vedtaksdata.vedtakstidspunkt)

            val andelerFraSanksjon = lagAndelerFraSanksjoner(vedtaksperioder, tilkjentYtelse)
            (tilkjentYtelse.andelerTilkjentYtelse + andelerFraSanksjon).sortedBy { it.stønadFom }.forEach { andel ->

                val vedtaksperiode = finnVedtaksperiodeForAndel(andel, vedtaksperioder)
                markerHistorikkEtterAndelSomFjernet(tilkjentYtelseMedVedtakstidspunkt, andel, vedtaksperiode, historikk)

                val andelFraHistorikk = finnTilsvarendeAndelIHistorikk(historikk, andel)
                val index = finnIndeksForNyAndel(historikk, andel)
                if (andelFraHistorikk == null) {
                    historikk.add(index, lagNyAndel(tilkjentYtelseMedVedtakstidspunkt, andel, vedtaksperiode))
                } else {
                    markerTidligereMedEndringOgReturnerNyAndel(tilkjentYtelseMedVedtakstidspunkt, andel, andelFraHistorikk, vedtaksperiode)
                        ?.let { historikk.add(index, it) }
                }
            }

            markerAndelerSomErFjernet(tilkjentYtelseMedVedtakstidspunkt, historikk)
        }
        return historikk
    }

    /**
     * Hvis en [andel] er oppdatert i denne [TilkjentYtelse] så
     * markeres historiske andeler som har fom etter denne fom som fjernet
     * Dette fordi vi alltid revurderer fra X dato, og allt etter det datoet blir overskrevet
     */
    private fun markerHistorikkEtterAndelSomFjernet(
        tilkjentYtelseMedVedtakstidspunkt: TilkjentYtelseMedVedtakstidspunkt,
        andel: AndelTilkjentYtelse,
        vedtaksperiode: Vedtakshistorikkperiode,
        historikk: MutableList<AndelHistorikkHolder>
    ) {
        val tilkjentYtelse = tilkjentYtelseMedVedtakstidspunkt.tilkjentYtelse
        if (!vedtaksperiode.erSanksjon && andel.kildeBehandlingId == tilkjentYtelse.behandlingId) {
            historikk.filter { it.andel.stønadFom > andel.stønadFom }
                .filter { it.endring == null || it.endring!!.type != EndringType.FJERNET }
                .forEach { it.endring = lagEndring(EndringType.FJERNET, tilkjentYtelseMedVedtakstidspunkt) }
        }
    }

    private fun lagAndelerFraSanksjoner(
        vedtaksperioder: List<Vedtakshistorikkperiode>,
        tilkjentYtelse: TilkjentYtelse
    ) =
        vedtaksperioder.filter { it.erSanksjon }.map {
            AndelTilkjentYtelse(
                beløp = 0,
                periode = it.periode,
                "",
                0,
                0,
                0,
                tilkjentYtelse.behandlingId
            )
        }

    /**
     * Markerer endrede med riktig type endret
     * Splitter eventuellt opp perioder som blivit avkortet, eks der stønadsbeløpet endret seg fra gitt måned
     */
    private fun markerTidligereMedEndringOgReturnerNyAndel(
        tilkjentYtelseMedVedtakstidspunkt: TilkjentYtelseMedVedtakstidspunkt,
        andel: AndelTilkjentYtelse,
        andelFraHistorikk: AndelHistorikkHolder,
        vedtaksperiode: Vedtakshistorikkperiode
    ): AndelHistorikkHolder? {
        // settes for å senere markere de som fjernet hvis de blir markert som endret
        andelFraHistorikk.kontrollert = tilkjentYtelseMedVedtakstidspunkt.tilkjentYtelse.id

        return andelFraHistorikk.finnEndringstype(andel, vedtaksperiode)?.let { endringType ->
            val andelHistorikk = andelFraHistorikk.andel
            andelFraHistorikk.endring = lagEndring(endringType, tilkjentYtelseMedVedtakstidspunkt)

            return if (endringType == EndringType.SPLITTET) {
                andelFraHistorikk.andel = andelHistorikk.copy(
                    stønadTom = andel.stønadTom,
                    kildeBehandlingId = andel.kildeBehandlingId
                )
                andelFraHistorikk.copy(
                    andel = andelHistorikk.copy(stønadFom = andel.stønadTom.plusDays(1)),
                    endring = lagEndring(EndringType.FJERNET, tilkjentYtelseMedVedtakstidspunkt)
                )
            } else {
                lagNyAndel(tilkjentYtelseMedVedtakstidspunkt, andel, vedtaksperiode)
            }
        }
    }

    private fun finnVedtaksperiodeForAndel(
        andel: AndelTilkjentYtelse,
        vedtaksperioder: List<Vedtakshistorikkperiode>
    ): Vedtakshistorikkperiode {
        return vedtaksperioder.first { it.periode.inneholder(andel.periode.fom) }
    }

    private fun sorterTilkjentYtelser(tilkjentYtelser: List<TilkjentYtelse>): List<TilkjentYtelse> =
        tilkjentYtelser.sortedBy { it.sporbar.opprettetTid }
            .map { it.copy(andelerTilkjentYtelse = it.andelerTilkjentYtelse.sortedBy(AndelTilkjentYtelse::stønadFom)) }

    private fun lagNyAndel(
        tilkjentYtelseMedVedtakstidspunkt: TilkjentYtelseMedVedtakstidspunkt,
        andel: AndelTilkjentYtelse,
        vedtaksperiode: Vedtakshistorikkperiode
    ) =
        AndelHistorikkHolder(
            behandlingId = tilkjentYtelseMedVedtakstidspunkt.tilkjentYtelse.behandlingId,
            vedtakstidspunkt = tilkjentYtelseMedVedtakstidspunkt.vedtakstidspunkt,
            saksbehandler = tilkjentYtelseMedVedtakstidspunkt.tilkjentYtelse.sporbar.opprettetAv,
            andel = andel,
            endring = null,
            vedtaksperiode = vedtaksperiode,
            kontrollert = tilkjentYtelseMedVedtakstidspunkt.tilkjentYtelse.id
        )

    private fun AndelHistorikkHolder.finnEndringstype(
        nyAndel: AndelTilkjentYtelse,
        nyPeriode: Vedtakshistorikkperiode
    ): EndringType? {
        return when {
            erSanksjonMedSammePeriode(nyAndel, nyPeriode) -> null
            aktivitetEllerPeriodeTypeHarEndretSeg(nyPeriode) -> EndringType.ERSTATTET
            this.andel.beløp != nyAndel.beløp -> EndringType.ERSTATTET
            this.andel.inntekt != nyAndel.inntekt -> EndringType.ERSTATTET
            erEndringerForBarnetilsyn(this.vedtaksperiode, nyPeriode) -> EndringType.ERSTATTET
            this.andel.stønadTom < nyAndel.stønadTom -> EndringType.ERSTATTET
            this.andel.stønadTom > nyAndel.stønadTom -> EndringType.SPLITTET
            this.andel.kildeBehandlingId != nyAndel.kildeBehandlingId -> EndringType.FJERNET
            else -> null // Uendret
        }
    }

    private fun erEndringerForBarnetilsyn(
        first: Vedtakshistorikkperiode,
        second: Vedtakshistorikkperiode
    ): Boolean {
        if (first !is VedtakshistorikkperiodeBarnetilsyn ||
            second !is VedtakshistorikkperiodeBarnetilsyn
        ) {
            return false
        }
        return first.antallBarn != second.antallBarn ||
            first.utgifter != second.utgifter ||
            first.kontantstøtte != second.kontantstøtte ||
            first.tilleggsstønad != second.tilleggsstønad
    }

    private fun AndelHistorikkHolder.erSanksjonMedSammePeriode(
        nyAndel: AndelTilkjentYtelse,
        nyPeriode: Vedtakshistorikkperiode
    ): Boolean {
        return this.vedtaksperiode.erSanksjon && nyPeriode.erSanksjon &&
            this.vedtaksperiode.periode == nyAndel.periode
    }

    private fun AndelHistorikkHolder.aktivitetEllerPeriodeTypeHarEndretSeg(annenVedtaksperiode: Vedtakshistorikkperiode): Boolean {
        val vedtaksperiode = this.vedtaksperiode
        if (vedtaksperiode.javaClass != annenVedtaksperiode.javaClass) {
            return true
        }
        // Kun for typkasting
        if (vedtaksperiode !is VedtakshistorikkperiodeOvergangsstønad ||
            annenVedtaksperiode !is VedtakshistorikkperiodeOvergangsstønad
        ) {
            return false
        }
        return vedtaksperiode.aktivitet != annenVedtaksperiode.aktivitet ||
            vedtaksperiode.periodeType != annenVedtaksperiode.periodeType
    }

    private fun lagEndring(type: EndringType, tilkjentYtelseMedVedtakstidspunkt: TilkjentYtelseMedVedtakstidspunkt) =
        HistorikkEndring(
            type = type,
            behandlingId = tilkjentYtelseMedVedtakstidspunkt.tilkjentYtelse.behandlingId,
            vedtakstidspunkt = tilkjentYtelseMedVedtakstidspunkt.vedtakstidspunkt
        )

    /**
     * Finner indeks for andelen etter [andel], hvis den ikke finnes returneres [historikk] sin size
     */
    private fun finnIndeksForNyAndel(
        historikk: List<AndelHistorikkHolder>,
        andel: AndelTilkjentYtelse
    ): Int {
        val index = historikk.indexOfFirst { it.andel.stønadFom > andel.stønadFom }
        return if (index == -1) historikk.size else index
    }

    private fun finnTilsvarendeAndelIHistorikk(
        historikk: List<AndelHistorikkHolder>,
        andel: AndelTilkjentYtelse
    ): AndelHistorikkHolder? =
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
    private fun markerAndelerSomErFjernet(
        tilkjentYtelseMedVedtakstidspunkt: TilkjentYtelseMedVedtakstidspunkt,
        historikk: MutableList<AndelHistorikkHolder>
    ) {
        val tilkjentYtelse = tilkjentYtelseMedVedtakstidspunkt.tilkjentYtelse
        historikk.filterNot { erAlleredeFjernetEllerKontrollert(it, tilkjentYtelse) }.forEach {
            it.endring = lagEndring(EndringType.FJERNET, tilkjentYtelseMedVedtakstidspunkt)
        }
    }

    private fun erAlleredeFjernetEllerKontrollert(
        historikk: AndelHistorikkHolder,
        tilkjentYtelse: TilkjentYtelse
    ) =
        historikk.endring?.type == EndringType.FJERNET ||
            historikk.endring?.type == EndringType.ERSTATTET ||
            historikk.kontrollert == tilkjentYtelse.id
}
