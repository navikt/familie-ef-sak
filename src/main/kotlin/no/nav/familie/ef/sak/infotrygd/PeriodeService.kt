package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infotrygd.InternPeriodeUtil.slåSammenPerioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriodeMedStønadstype
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Component
import java.time.YearMonth
import java.util.UUID

@Component
class PeriodeService(
    private val personService: PersonService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val infotrygdService: InfotrygdService,
    private val vedtakService: VedtakService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val barnService: BarnService,
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

    fun hentLøpendeOvergangsstønadPerioderMedAktivitetOgBehandlingsbarn(
        personIdent: String,
    ): List<PeriodeMedAktivitetOgBarn> =
        fagsakService
            .finnFagsak(setOf(personIdent), StønadType.OVERGANGSSTØNAD)
            ?.let { behandlingService.finnSisteIverksatteBehandling(it.id) }
            ?.let { behandling ->
                val andelerTilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandling.id).andelerTilkjentYtelse
                tilArbeidsoppfølgingsPeriode(andelerTilkjentYtelse)
            } ?: emptyList()

    private fun tilInternPeriode(tilkjentYtelse: TilkjentYtelse) =
        tilkjentYtelse.andelerTilkjentYtelse
            .map(AndelTilkjentYtelse::tilInternPeriode)
            // trenger å sortere de revers pga filtrerOgSorterPerioderFraInfotrygd gjør det,
            // då vi ønsker de sortert på siste hendelsen først
            .sortedWith(compareBy<InternPeriode> { it.stønadFom }.reversed())

    private fun tilArbeidsoppfølgingsPeriode(andelerTilkjentYtelse: List<AndelTilkjentYtelse>): List<PeriodeMedAktivitetOgBarn> =
        andelerTilkjentYtelse
            .filter { it.harPeriodeSomLøperNåEllerIFramtid() }
            .mapNotNull { andel ->
                val andelsVedtak = finnVedtaksperiodeforAndel(andel)
                when (andelsVedtak) {
                    null -> throw IllegalStateException("Fant ikke vedtaksperiode for andel ${andel.periode} med behandlingId ${andel.kildeBehandlingId}")
                    else -> lagOvergangsstønadperiodeMedAktivitet(andel, andelsVedtak)
                }
            }.sortedBy { it.stønadFraOgMed }

    private fun lagOvergangsstønadperiodeMedAktivitet(
        andel: AndelTilkjentYtelse,
        andelsVedtak: Vedtaksperiode,
    ): PeriodeMedAktivitetOgBarn? {
        val behandlingsbarn = finnBehandlingsbarnMedOppfyltAleneomsorgvilkår(andel.kildeBehandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId = andel.kildeBehandlingId)

        return PeriodeMedAktivitetOgBarn(
            behandlingId = behandling.eksternId,
            stønadFraOgMed = andel.stønadFom,
            stønadTilOgMed = andel.stønadTom,
            aktivitet = andelsVedtak.aktivitet,
            periodeType = andelsVedtak.periodeType,
            barn = behandlingsbarn.map { BehandlingsbarnMedOppfyltAleneomsorg(personIdent = it.personIdent, fødselTermindato = it.fødselTermindato) },
            harAktivitetsplikt = andelsVedtak.aktivitet.harAktivitetsplikt(),
        )
    }

    private fun AktivitetType.harAktivitetsplikt(): Boolean =
        when (this) {
            AktivitetType.FORSØRGER_I_ARBEID,
            AktivitetType.FORSØRGER_I_UTDANNING,
            AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER,
            AktivitetType.FORSØRGER_ETABLERER_VIRKSOMHET,
            AktivitetType.UTVIDELSE_FORSØRGER_I_UTDANNING,
            AktivitetType.FORLENGELSE_STØNAD_UT_SKOLEÅRET -> true
            AktivitetType.MIGRERING ,
            AktivitetType.IKKE_AKTIVITETSPLIKT ,
            AktivitetType.BARN_UNDER_ETT_ÅR ,
            AktivitetType.BARNET_SÆRLIG_TILSYNSKREVENDE ,
            AktivitetType.FORSØRGER_MANGLER_TILSYNSORDNING ,
            AktivitetType.FORSØRGER_ER_SYK ,
            AktivitetType.BARNET_ER_SYKT ,
            AktivitetType.UTVIDELSE_BARNET_SÆRLIG_TILSYNSKREVENDE ,
            AktivitetType.FORLENGELSE_MIDLERTIDIG_SYKDOM ,
            AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID ,
            AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER ,
            AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_OPPSTART_KVALIFISERINGSPROGRAM ,
            AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_TILSYNSORDNING ,
            AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_UTDANNING -> false
        }

    fun finnVedtaksperiodeforAndel(andel: AndelTilkjentYtelse): Vedtaksperiode? {
        val vedtaksperioder: List<Vedtaksperiode> = finnVedtaksperioder(andel.kildeBehandlingId)
        return vedtaksperioder.find { vedtaksperiode ->
            vedtaksperiode.månedsPeriode().inneholder(andel.periode)
        }
    }

    private fun finnVedtaksperioder(behandlingId: UUID): List<Vedtaksperiode> {
        val vedtak: Vedtak = vedtakService.hentVedtak(behandlingId)
        return vedtak.perioder?.perioder?.filter { it.periodeType != VedtaksperiodeType.MIDLERTIDIG_OPPHØR } ?: emptyList()
    }

    private fun finnBehandlingsbarnMedOppfyltAleneomsorgvilkår(behandlingsId: UUID): List<BehandlingBarn> {
        val vilkårsvurderinger =
            vilkårsvurderingRepository.findByTypeAndBehandlingIdIn(VilkårType.ALENEOMSORG, listOf(behandlingsId))
        val barnMedAleneomsorg =
            vilkårsvurderinger.filter { it.resultat == Vilkårsresultat.OPPFYLT }.mapNotNull { vilkårsvurdering ->
                vilkårsvurdering.barnId
            }
        return barnService.hentBehandlingBarnForBarnIder(barnMedAleneomsorg)
    }
}

fun AndelTilkjentYtelse.harPeriodeSomLøperNåEllerIFramtid(): Boolean {
    val nå = Månedsperiode(YearMonth.now())
    val periode = Månedsperiode(this.stønadFom, this.stønadTom)
    return nå.overlapper(periode) || periode.tom > nå.tom
}

private fun Vedtaksperiode.månedsPeriode() = Månedsperiode(datoFra, datoTil)

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
