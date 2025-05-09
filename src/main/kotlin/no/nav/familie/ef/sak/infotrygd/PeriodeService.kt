package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infotrygd.InternPeriodeUtil.slåSammenPerioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriodeMedStønadstype
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Component

@Component
class PeriodeService(
    private val personService: PersonService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val infotrygdService: InfotrygdService,
    private val vedtakService: VedtakService,
) {
    fun hentPerioderFraEfOgInfotrygd(personIdent: String): InternePerioder {
        val personIdenter = personService.hentPersonIdenter(personIdent).identer()
        val perioderFraReplika = infotrygdService.hentSammenslåttePerioderSomInternPerioder(personIdenter)

        return InternePerioder(
            overgangsstønad =
                slåSammenPerioder(
                    hentPerioderFraEf(personIdenter, StønadType.OVERGANGSSTØNAD),
                    perioderFraReplika.overgangsstønad,
                ),
            barnetilsyn =
                slåSammenPerioder(
                    hentPerioderFraEf(personIdenter, StønadType.BARNETILSYN),
                    perioderFraReplika.barnetilsyn,
                ),
            skolepenger =
                slåSammenPerioder(
                    hentPerioderFraEf(personIdenter, StønadType.SKOLEPENGER),
                    perioderFraReplika.skolepenger,
                ),
        )
    }

    fun hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent: String): List<InternPeriode> {
        val personIdenter = personService.hentPersonIdenter(personIdent).identer()
        val perioderFraReplika =
            infotrygdService.hentSammenslåttePerioderSomInternPerioder(personIdenter).overgangsstønad
        val perioderFraEf = hentPerioderFraEf(personIdenter, StønadType.OVERGANGSSTØNAD)

        return slåSammenPerioder(perioderFraEf, perioderFraReplika)
    }

    fun hentPerioderForBarnetilsynFraEfOgInfotrygd(personIdent: String): List<InternPeriode> {
        val personIdenter = personService.hentPersonIdenter(personIdent).identer()
        val perioderFraReplika =
            infotrygdService.hentSammenslåttePerioderSomInternPerioder(personIdenter).barnetilsyn
        val perioderFraEf = hentPerioderFraEf(personIdenter, StønadType.BARNETILSYN)

        return slåSammenPerioder(perioderFraEf, perioderFraReplika)
    }

    fun hentPeriodeFraVedtakForSkolepenger(personIdent: String): List<EksternPeriodeMedStønadstype> {
        val personIdenter = personService.hentPersonIdenter(personIdent).identer()
        val skoleårsperioder =
            fagsakService
                .finnFagsak(personIdenter, StønadType.SKOLEPENGER)
                ?.let { behandlingService.finnSisteIverksatteBehandling(it.id) }
                ?.let { vedtakService.hentVedtak(it.id) }
                ?.let { it.skolepenger?.skoleårsperioder }

        val skoleårsperioderList =
            skoleårsperioder
                ?.flatMap { it.perioder }
                ?.map {
                    EksternPeriodeMedStønadstype(
                        fomDato = it.datoFra,
                        tomDato = it.datoTil,
                        StønadType.SKOLEPENGER,
                    )
                } ?: emptyList()

        return skoleårsperioderList
    }

    private fun hentPerioderFraEf(
        personIdenter: Set<String>,
        stønadstype: StønadType,
    ): EfInternPerioder? =
        fagsakService
            .finnFagsak(personIdenter, stønadstype)
            ?.let { behandlingService.finnSisteIverksatteBehandling(it.id) }
            ?.let { behandling ->
                val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandling.id)
                val internperioder = tilInternPeriode(tilkjentYtelse)
                val startdato = tilkjentYtelse.startdato
                EfInternPerioder(startdato, internperioder)
            }

    // TODO gjør denne private og kall ny metode som slår sammen med infotrygdperioder
    fun hentPerioderFraEfMedAktivitet(
        personIdenter: Set<String>,
        stønadstype: StønadType,
    ): EfInternPerioderMedAktivitet? =
        fagsakService
            .finnFagsak(personIdenter, stønadstype)
            ?.let { behandlingService.finnSisteIverksatteBehandling(it.id) }
            ?.let { behandling ->
                val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandling.id)
                val internperioder = tilInternPeriodeMedAktivitet(tilkjentYtelse)
                val startdato = tilkjentYtelse.startdato
                EfInternPerioderMedAktivitet(startdato, internperioder)
            }

    private fun tilInternPeriode(tilkjentYtelse: TilkjentYtelse) =
        tilkjentYtelse.andelerTilkjentYtelse
            .map(AndelTilkjentYtelse::tilInternPeriode)
            // trenger å sortere de revers pga filtrerOgSorterPerioderFraInfotrygd gjør det,
            // då vi ønsker de sortert på siste hendelsen først
            .sortedWith(compareBy<InternPeriode> { it.stønadFom }.reversed())

    private fun tilInternPeriodeMedAktivitet(tilkjentYtelse: TilkjentYtelse): List<InternPeriodeMedAktivitet> {

        // TODO mattis - mulig vi må sortede denne også
        return tilkjentYtelse.andelerTilkjentYtelse.map { andel -> lagInternPeriodeMedAktivitet(andel) }
    }

    private fun lagInternPeriodeMedAktivitet(andel: AndelTilkjentYtelse): InternPeriodeMedAktivitet {

        val vedtak : Vedtak = vedtakService.hentVedtak(andel.kildeBehandlingId)
        val vedtaksperioder : List<Vedtaksperiode> = vedtak.perioder?.perioder?.filter { it.periodeType != no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType.MIDLERTIDIG_OPPHØR } ?: emptyList()

        // TODO TEST!!!
        val vedtaksperiodeSomMatcherMedAndel = vedtaksperioder.find {
            andel.periode.overlapper(it.månedsPeriode())
        }

        return InternPeriodeMedAktivitet(
            personIdent = andel.personIdent,
            inntektsreduksjon = andel.inntektsreduksjon,
            samordningsfradrag = andel.samordningsfradrag,
            utgifterBarnetilsyn = 0, // andel.utgifterBarnetilsyn TODO
            månedsbeløp = andel.beløp,
            engangsbeløp = andel.beløp,
            stønadFom = andel.stønadFom,
            stønadTom = andel.stønadTom,
            opphørsdato = null,
            datakilde = Datakilde.EF,
            aktivitet = vedtaksperiodeSomMatcherMedAndel?.aktivitet,
            periodeType = vedtaksperiodeSomMatcherMedAndel?.periodeType,
        )
    }
}

private fun Vedtaksperiode.månedsPeriode() =
    Månedsperiode(datoFra, datoTil)


private fun AndelTilkjentYtelse.tilInternPeriode(): InternPeriode =
    InternPeriode(
        personIdent = this.personIdent,
        inntektsreduksjon = this.inntektsreduksjon,
        samordningsfradrag = this.samordningsfradrag,
        utgifterBarnetilsyn = 0, // this.utgifterBarnetilsyn TODO
        månedsbeløp = this.beløp,
        engangsbeløp = this.beløp,
        stønadFom = this.stønadFom,
        stønadTom = this.stønadTom,
        opphørsdato = null,
        datakilde = Datakilde.EF,
    )

private fun Vedtaksperiode.getPeriode(): Månedsperiode {
    return Månedsperiode(this.datoFra, datoTil)
}



fun InfotrygdPeriode.tilInternPeriode(): InternPeriode =
    InternPeriode(
        personIdent = this.personIdent,
        inntektsreduksjon = this.inntektsreduksjon,
        samordningsfradrag = this.samordningsfradrag,
        utgifterBarnetilsyn = this.utgifterBarnetilsyn,
        månedsbeløp = this.månedsbeløp,
        engangsbeløp = this.engangsbeløp,
        stønadFom = this.stønadFom,
        stønadTom = this.stønadTom,
        opphørsdato = this.opphørsdato,
        datakilde = Datakilde.INFOTRYGD,
    )
