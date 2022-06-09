package no.nav.familie.ef.sak.ekstern.bisys

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.historikk.erIkkeFjernet
import no.nav.familie.eksterne.kontrakter.bisys.BarnetilsynBisysPeriode
import no.nav.familie.eksterne.kontrakter.bisys.BarnetilsynBisysResponse
import no.nav.familie.eksterne.kontrakter.bisys.Datakilde
import no.nav.familie.eksterne.kontrakter.bisys.Periode
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BisysBarnetilsynService(
    private val personService: PersonService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val barnService: BarnService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val infotrygdService: InfotrygdService
) {

    fun hentBarnetilsynperioderFraEfOgInfotrygd(personIdent: String, fomDato: LocalDate): BarnetilsynBisysResponse {
        return BarnetilsynBisysResponse(kombinerBarnetilsynperioderFraEfOgInfotrygd(personIdent, fomDato))
    }

    private fun kombinerBarnetilsynperioderFraEfOgInfotrygd(
        personIdent: String,
        fomDato: LocalDate
    ): List<BarnetilsynBisysPeriode> {

        val infotrygdperioder = hentInfotrygdPerioderBarnetilsyn(personIdent, fomDato)
        val perioderBarnetilsyn = hentPerioderBarnetilsyn(personIdent, fomDato)
        return slåSammenPerioder(
            infotrygdPerioder = infotrygdperioder,
            efPerioder = perioderBarnetilsyn.second,
            startdato = perioderBarnetilsyn.first
        )
    }

    private fun hentPerioderBarnetilsyn(
        personIdent: String,
        fomDato: LocalDate
    ): Pair<LocalDate?, List<BarnetilsynBisysPeriode>> {
        val personIdenter = personService.hentPersonIdenter(personIdent).identer()
        val fagsak: Fagsak = fagsakService.finnFagsak(personIdenter, StønadType.BARNETILSYN)
            ?: return Pair(null, emptyList())
        val sisteGjeldendeBehandling =
            behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id) ?: return Pair(
                null,
                emptyList()
            )
        val startdato = tilkjentYtelseService.hentForBehandling(
            sisteGjeldendeBehandling.id
        ).startdato

        val historikk = tilkjentYtelseService.hentHistorikk(fagsak.id, null)
            .filter { it.erIkkeFjernet() }
            .filter { it.andel.beløp > 0 && it.andel.stønadTil >= fomDato }

        val barnIdenter = historikk.flatMap { it.andel.barn }
            .distinct()
            .let { barnService.hentBehandlingBarnForBarnIder(it) }
            .associate { it.id to it.personIdent }

        val barnetilsynBisysPerioder = historikk.map { andel ->
            BarnetilsynBisysPeriode(
                Periode(andel.andel.stønadFra, andel.andel.stønadTil),
                andel.andel.barn.map {
                    barnIdenter[it]
                        ?: error("Fant ingen personident for barn=$it")
                },
                andel.andel.beløp,
                Datakilde.EF
            )
        }
        return Pair(startdato, barnetilsynBisysPerioder.sortedBy { it.periode.fom })
    }

    private fun hentInfotrygdPerioderBarnetilsyn(
        personIdent: String,
        fomDato: LocalDate
    ): List<BarnetilsynBisysPeriode> {
        return infotrygdService.hentSammenslåttePerioderFraReplika(
            personIdent,
            StønadType.BARNETILSYN
        ).filter { it.stønadTom >= fomDato }
            .map { periode ->
                BarnetilsynBisysPeriode(
                    Periode(periode.stønadFom, periode.stønadTom),
                    periode.barnIdenter,
                    periode.månedsbeløp,
                    Datakilde.INFOTRYGD
                )
            }
    }

    private fun slåSammenPerioder(
        infotrygdPerioder: List<BarnetilsynBisysPeriode>,
        efPerioder: List<BarnetilsynBisysPeriode>,
        startdato: LocalDate?
    ): List<BarnetilsynBisysPeriode> {
        if (startdato == null) {
            return infotrygdPerioder
        }
        val perioderFraInfotrygdSomBeholdes = infotrygdPerioder.mapNotNull {
            if (it.periode.fom >= startdato) {
                null
            } else if (it.periode.tom > startdato) {
                it.copy(periode = it.periode.copy(tom = startdato.minusDays(1)))
            } else {
                it
            }
        }
        return (perioderFraInfotrygdSomBeholdes + efPerioder).sortedBy { it.periode.fom }
    }
}
