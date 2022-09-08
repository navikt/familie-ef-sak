package no.nav.familie.ef.sak.ekstern.bisys

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
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
    private val andelsHistorikkService: AndelsHistorikkService,
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
            efPerioder = perioderBarnetilsyn
        )
    }

    private fun hentPerioderBarnetilsyn(
        personIdent: String,
        fomDato: LocalDate
    ): EfPerioder? {
        val personIdenter = personService.hentPersonIdenter(personIdent).identer()
        val fagsak: Fagsak = fagsakService.finnFagsak(personIdenter, StønadType.BARNETILSYN) ?: return null
        val sisteGjeldendeBehandling =
            behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id) ?: return null
        val startdato = tilkjentYtelseService.hentForBehandling(sisteGjeldendeBehandling.id).startdato

        val historikk = andelsHistorikkService.hentHistorikk(fagsak.id, null)
            .filter { it.erIkkeFjernet() }
            .filter { it.andel.beløp > 0 && it.andel.periode.tomDato >= fomDato }

        val barnIdenter = historikk.flatMap { it.andel.barn }
            .distinct()
            .let { barnService.hentBehandlingBarnForBarnIder(it) }
            .associate { it.id to it.personIdent }

        val barnetilsynBisysPerioder = historikk.map { andel ->
            BarnetilsynBisysPeriode(
                Periode(andel.andel.periode.fomDato, andel.andel.periode.tomDato),
                andel.andel.barn.map {
                    barnIdenter[it] ?: error("Fant ingen personident for barn=$it")
                },
                andel.andel.beløp,
                Datakilde.EF
            )
        }
        return EfPerioder(startdato, barnetilsynBisysPerioder.sortedBy { it.periode.fom })
    }

    private fun hentInfotrygdPerioderBarnetilsyn(
        personIdent: String,
        fomDato: LocalDate
    ): List<BarnetilsynBisysPeriode> {
        return infotrygdService.hentSammenslåtteBarnetilsynPerioderFraReplika(personIdent)
            .filter { it.stønadTom >= fomDato }
            .map { periode ->
                BarnetilsynBisysPeriode(
                    periode = Periode(periode.stønadFom, periode.stønadTom),
                    barnIdenter = periode.barnIdenter,
                    månedsbeløp = periode.månedsbeløp,
                    datakilde = Datakilde.INFOTRYGD
                )
            }
    }

    private fun slåSammenPerioder(
        infotrygdPerioder: List<BarnetilsynBisysPeriode>,
        efPerioder: EfPerioder?
    ): List<BarnetilsynBisysPeriode> {
        if (efPerioder == null) {
            return infotrygdPerioder
        }
        val startdato = efPerioder.startdato
        val perioder = efPerioder.perioder

        val perioderFraInfotrygdSomBeholdes = infotrygdPerioder.mapNotNull {
            if (it.periode.fom >= startdato) {
                null
            } else if (it.periode.tom > startdato) {
                it.copy(periode = it.periode.copy(tom = startdato.minusDays(1)))
            } else {
                it
            }
        }
        return (perioderFraInfotrygdSomBeholdes + perioder).sortedBy { it.periode.fom }
    }

    data class EfPerioder(val startdato: LocalDate, val perioder: List<BarnetilsynBisysPeriode>)
}
