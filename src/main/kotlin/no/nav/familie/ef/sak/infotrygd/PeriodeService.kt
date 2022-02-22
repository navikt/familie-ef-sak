package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.infotrygd.InternPeriodeUtil.slåSammenPerioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import org.springframework.stereotype.Component

@Component
class PeriodeService(
        private val pdlClient: PdlClient,
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val tilkjentYtelseService: TilkjentYtelseService,
        private val infotrygdService: InfotrygdService
) {

    fun hentPerioderFraEfOgInfotrygd(personIdent: String): InternePerioder {
        val personIdenter = pdlClient.hentPersonidenter(personIdent, true).identer()
        val perioderFraReplika = infotrygdService.hentSammenslåttePerioderSomInternPerioder(personIdenter)

        return InternePerioder(
                overgangsstønad = slåSammenPerioder(hentPerioderFraEf(personIdenter, Stønadstype.OVERGANGSSTØNAD),
                                                    perioderFraReplika.overgangsstønad),
                barnetilsyn = slåSammenPerioder(hentPerioderFraEf(personIdenter, Stønadstype.BARNETILSYN),
                                                perioderFraReplika.barnetilsyn),
                skolepenger = slåSammenPerioder(hentPerioderFraEf(personIdenter, Stønadstype.SKOLEPENGER),
                                                perioderFraReplika.skolepenger)
        )
    }

    fun hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent: String): List<InternPeriode> {
        val personIdenter = pdlClient.hentPersonidenter(personIdent, true).identer()
        val perioderFraReplika = infotrygdService.hentSammenslåttePerioderSomInternPerioder(personIdenter).overgangsstønad
        val perioderFraEf = hentPerioderFraEf(personIdenter, Stønadstype.OVERGANGSSTØNAD)

        return slåSammenPerioder(perioderFraEf, perioderFraReplika)
    }

    private fun hentPerioderFraEf(personIdenter: Set<String>, stønadstype: Stønadstype): List<InternPeriode> {
        return fagsakService.finnFagsak(personIdenter, stønadstype)
                       ?.let { behandlingService.finnSisteIverksatteBehandling(it.id) }
                       ?.let { hentPerioderFraEf(it) }
                       // trenger å sortere de revers pga filtrerOgSorterPerioderFraInfotrygd gjør det,
                       // då vi ønsker de sortert på siste hendelsen først
                       ?.sortedWith(compareBy<InternPeriode> { it.stønadFom }.reversed())
               ?: emptyList()
    }

    private fun hentPerioderFraEf(it: Behandling): List<InternPeriode> =
            tilkjentYtelseService.hentForBehandling(it.id).andelerTilkjentYtelse
                    .map(AndelTilkjentYtelse::tilInternPeriode)
}

private fun AndelTilkjentYtelse.tilInternPeriode(): InternPeriode = InternPeriode(
        personIdent = this.personIdent,
        inntektsreduksjon = this.inntektsreduksjon,
        samordningsfradrag = this.samordningsfradrag,
        utgifterBarnetilsyn = 0, // this.utgifterBarnetilsyn TODO
        månedsbeløp = this.beløp,
        engangsbeløp = this.beløp,
        stønadFom = this.stønadFom,
        stønadTom = this.stønadTom,
        opphørsdato = null,
        datakilde = PeriodeOvergangsstønad.Datakilde.EF
)

fun InfotrygdPeriode.tilInternPeriode(): InternPeriode = InternPeriode(
        personIdent = this.personIdent,
        inntektsreduksjon = this.inntektsreduksjon,
        samordningsfradrag = this.samordningsfradrag,
        utgifterBarnetilsyn = this.utgifterBarnetilsyn,
        månedsbeløp = this.månedsbeløp,
        engangsbeløp = this.engangsbeløp,
        stønadFom = this.stønadFom,
        stønadTom = this.stønadTom,
        opphørsdato = this.opphørsdato,
        datakilde = PeriodeOvergangsstønad.Datakilde.INFOTRYGD
)
