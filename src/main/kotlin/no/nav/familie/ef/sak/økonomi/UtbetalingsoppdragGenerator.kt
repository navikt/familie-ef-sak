package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.integration.FAGSYSTEM
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.util.hasList
import no.nav.familie.ef.sak.util.mergeMultiMap
import no.nav.familie.ef.sak.økonomi.ØkonomiUtils.andelTilOpphørMedDato
import no.nav.familie.ef.sak.økonomi.ØkonomiUtils.andelerTilOpprettelse
import no.nav.familie.ef.sak.økonomi.ØkonomiUtils.beståendeAndelerPerKjede
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.ENDR
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.NY
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object UtbetalingsoppdragGenerator {

    /**
     * Lager utbetalingsoppdrag med kjedede perioder av andeler.
     * Ved opphør sendes @param[nyTilkjentYtelseMedMetaData] uten andeler.
     *
     * @param[nyTilkjentYtelseMedMetaData] Den nye tilkjente ytelsen, med fullstending sett av andeler
     * @param[forrigeTilkjentYtelse] Forrige tilkjent ytelse, med fullstendig sett av andeler med id
     * @return Ny tilkjent ytelse med andeler med id'er, samt utbetalingsoppdrag
     */
    fun lagTilkjentYtelseMedUtbetalingsoppdrag(nyTilkjentYtelseMedMetaData: TilkjentYtelseMedMetaData,
                                               forrigeTilkjentYtelse: TilkjentYtelse? = null): TilkjentYtelse {

        val nyTilkjentYtelse = nyTilkjentYtelseMedMetaData.tilkjentYtelse
        val oppdaterteKjeder = lagKjederUtenNullVerdier(nyTilkjentYtelse)
        val forrigeKjeder = lagKjederUtenNullVerdier(forrigeTilkjentYtelse)
        val sistePeriodeIder = sistePeriodeIder(forrigeTilkjentYtelse)

        val aksjonskodePåOppdragsnivå =
                if (forrigeTilkjentYtelse == null) NY
                else ENDR

        val beståendeAndelerIHverKjede = beståendeAndelerPerKjede(forrigeKjeder, oppdaterteKjeder)
        val andelerTilOpphør = andelTilOpphørMedDato(forrigeKjeder, oppdaterteKjeder)
        val andelerTilOpprettelse = andelerTilOpprettelse(oppdaterteKjeder, beståendeAndelerIHverKjede)

        val andelerTilOpprettelseMedPeriodeIder = lagAndelerMedPeriodeIder(andelerTilOpprettelse, sistePeriodeIder)

        val utbetalingsperioderSomOpprettes =
                lagUtbetalingsperioderForOpprettelse(andeler = andelerTilOpprettelseMedPeriodeIder,
                                                     behandlingId = nyTilkjentYtelseMedMetaData.eksternBehandlingId,
                                                     tilkjentYtelse = nyTilkjentYtelse)

        val utbetalingsperioderSomOpphøres =
                lagUtbetalingsperioderForOpphør(andeler = andelerTilOpphør,
                                                tilkjentYtelse = nyTilkjentYtelse,
                                                behandlingId = nyTilkjentYtelseMedMetaData.eksternBehandlingId
                )

        val utbetalingsoppdrag =
                Utbetalingsoppdrag(saksbehandlerId = nyTilkjentYtelse.saksbehandler,
                                   kodeEndring = aksjonskodePåOppdragsnivå,
                                   fagSystem = FAGSYSTEM,
                                   saksnummer = nyTilkjentYtelseMedMetaData.eksternFagsakId.toString(),
                                   aktoer = nyTilkjentYtelse.personident,
                        //TODO Trunkert avstemmingstidspunkt for å kunne skape forutsigbarhet mtp tester.
                        // Tester vil kunne feile hver nye time
                        //TODO Løsning kan være å putte avstemmingstidspunkt også i TilkjentYtelse.
                                   avstemmingTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
                                   utbetalingsperiode = listOf(utbetalingsperioderSomOpprettes,
                                                               utbetalingsperioderSomOpphøres)
                                           .flatten()
                                           .sortedBy { it.periodeId }
                )

        val gjeldendeAndeler = mergeMultiMap(beståendeAndelerIHverKjede, andelerTilOpprettelseMedPeriodeIder)

        // Hvis det ikke er noen andeler igjen, må vi opprette en "null-andel" som tar vare på periodeId'en for ytelsestypen
        // På toppen av metoden filtrerer vi bort disse når vi bygger kjedene, men bruker dem til å finne siste periodeId
        val nullAndeler = sistePeriodeIder
                .filterKeys { !gjeldendeAndeler.hasList(it) }
                .mapValues { (kjedeId, periodeId) -> listOf(kjedeId.tilNullAndelTilkjentYtelse(periodeId)) }

        return nyTilkjentYtelse.copy(utbetalingsoppdrag = utbetalingsoppdrag,
                                     stønadFom = gjeldendeAndeler.values.flatten().minOfOrNull { it.stønadFom },
                                     stønadTom = gjeldendeAndeler.values.flatten().maxOfOrNull { it.stønadTom },
                                     andelerTilkjentYtelse = mergeMultiMap(gjeldendeAndeler, nullAndeler).values.flatten())
        //TODO legge til startperiode, sluttperiode, opphørsdato. Se i BA-sak - legges på i konsistensavstemming?
    }

    private fun lagUtbetalingsperioderForOpphør(andeler: Map<KjedeId, Pair<AndelTilkjentYtelse, LocalDate>>,
                                                behandlingId: Long,
                                                tilkjentYtelse: TilkjentYtelse): List<Utbetalingsperiode> {
        val utbetalingsperiodeMal = UtbetalingsperiodeMal(tilkjentYtelse, true)
        return andeler.values.map { (sisteAndelIKjede, opphørKjedeFom) ->
            utbetalingsperiodeMal.lagPeriodeFraAndel(andel = sisteAndelIKjede,
                                                     behandlingId = behandlingId,
                                                     opphørKjedeFom = opphørKjedeFom)
        }
    }

    private fun lagUtbetalingsperioderForOpprettelse(andeler: Map<KjedeId, List<AndelTilkjentYtelse>>,
                                                     behandlingId: Long,
                                                     tilkjentYtelse: TilkjentYtelse): List<Utbetalingsperiode> {

        val utbetalingsperiodeMal = UtbetalingsperiodeMal(tilkjentYtelse)
        return andeler
                .flatMap { (_, andeler) -> andeler }
                .map {
                    utbetalingsperiodeMal.lagPeriodeFraAndel(it, behandlingId)
                }
    }

    private fun lagAndelerMedPeriodeIder(andeler: Map<KjedeId, List<AndelTilkjentYtelse>>,
                                         sisteOffsetIKjedeOversikt: Map<KjedeId, PeriodeId?>)
            : Map<KjedeId, List<AndelTilkjentYtelse>> {
        return andeler.filter { (_, andeler) -> andeler.isNotEmpty() }
                .mapValues { (kjedeId, kjede: List<AndelTilkjentYtelse>) ->
                    val forrigePeriodeIdIKjede: Long? = sisteOffsetIKjedeOversikt[kjedeId]?.gjeldende
                    val nestePeriodeIdIKjede = forrigePeriodeIdIKjede?.plus(1) ?: 1

                    kjede.sortedBy { it.stønadFom }.mapIndexed { index, andel ->

                        andel.copy(periodeId = nestePeriodeIdIKjede + index,
                                   forrigePeriodeId = if (index == 0) forrigePeriodeIdIKjede
                                   else nestePeriodeIdIKjede + index - 1)
                    }
                }
    }

    private fun sistePeriodeIder(tilkjentYtelse: TilkjentYtelse?): Map<KjedeId, PeriodeId?> {
        return tilkjentYtelse?.let { ytelse ->
            ytelse.andelerTilkjentYtelse
                    .groupBy { it.tilKjedeId() }
                    .mapValues { (_, kjede) ->
                        kjede.filter { it.periodeId != null }.maxByOrNull { it.periodeId!! }?.tilPeriodeId()
                    }
        } ?: emptyMap()
    }

    private fun lagKjederUtenNullVerdier(tilkjentYtelse: TilkjentYtelse?): Map<KjedeId, List<AndelTilkjentYtelse>> =
            tilkjentYtelse?.andelerTilkjentYtelse
                    ?.filter { !it.erNull() }
                    ?.groupBy { it.tilKjedeId() } ?: emptyMap()
}