package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import org.springframework.stereotype.Component

@Component
class PeriodeService(
    private val pdlClient: PdlClient,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val replikaClient: InfotrygdReplikaClient
) {

    fun hentPerioderFraEfOgInfotrygd(personIdent: String): List<InternPeriode> {
        val personIdenter = pdlClient.hentPersonidenter(personIdent, true).identer()
        val perioderFraReplika = hentPerioderFraReplika(personIdenter)
        val perioderFraEf = hentPerioderFraEf(personIdenter)

        return InternPeriodeUtil.slåSammenPerioder(perioderFraReplika + perioderFraEf)
    }

    private fun hentPerioderFraEf(personIdenter: Set<String>): List<InternPeriode> {
        return fagsakService.finnFagsak(personIdenter, Stønadstype.OVERGANGSSTØNAD)
            ?.let { behandlingService.finnSisteIverksatteBehandling(it.id) }
            ?.let { hentPerioderFraEf(it) }
            ?: emptyList()
    }

    private fun hentPerioderFraReplika(personIdenter: Set<String>): List<InternPeriode> {
        val perioder =
            replikaClient.hentPerioder(InfotrygdPeriodeRequest(personIdenter, setOf(StønadType.OVERGANGSSTØNAD))).overgangsstønad
        val filtrertPerioder = InfotrygdPeriodeUtil.filtrerOgSorterPerioderFraInfotrygd(perioder)
        return filtrertPerioder.map(InfotrygdPeriode::tilInternPeriode)
    }

    private fun hentPerioderFraEf(it: Behandling): List<InternPeriode> =
        tilkjentYtelseService.hentForBehandling(it.id).andelerTilkjentYtelse
            .map(AndelTilkjentYtelse::tilInternPeriode)
}

private fun AndelTilkjentYtelse.tilInternPeriode(): InternPeriode = InternPeriode(
    inntektsreduksjon = this.inntektsreduksjon,
    samordningsfradrag = this.samordningsfradrag,
    beløp = this.beløp,
    stønadFom = this.stønadFom,
    stønadTom = this.stønadFom,
    opphørsdato = null,
    datakilde = PeriodeOvergangsstønad.Datakilde.EF
)

fun InfotrygdPeriode.tilInternPeriode(): InternPeriode = InternPeriode(
    inntektsreduksjon = this.inntektsreduksjon,
    samordningsfradrag = this.samordningsfradrag,
    beløp = this.beløp,
    stønadFom = this.stønadFom,
    stønadTom = this.stønadTom,
    opphørsdato = this.opphørsdato,
    datakilde = PeriodeOvergangsstønad.Datakilde.INFOTRYGD
)
