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
import no.nav.familie.ef.sak.vedtak.historikk.erAktivVedtaksperiode
import no.nav.familie.eksterne.kontrakter.bisys.BarnetilsynBisysPeriode
import no.nav.familie.eksterne.kontrakter.bisys.BarnetilsynBisysResponse
import no.nav.familie.eksterne.kontrakter.bisys.Periode
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

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
        val barnetilsynBisysPerioder = kombinerBarnetilsynperioderFraEfOgInfotrygd(personIdent, fomDato)
        return BarnetilsynBisysResponse(barnetilsynBisysPerioder.mergeSammenhengendePerioder())
    }

    fun List<BarnetilsynBisysPeriode>.mergeSammenhengendePerioder(): List<BarnetilsynBisysPeriode> {
        val sortertPåDatoListe = this.sortedBy { it.periode.fom }
        return sortertPåDatoListe.fold(mutableListOf()) { acc, entry ->
            val last = acc.lastOrNull()
            if (last != null && last.hengerSammenMed(entry) && last.harSammeBarn(entry)) {
                acc.removeLast()
                acc.add(
                    last.copy(
                        periode = last.periode.union(entry.periode)
                    )
                )
            } else {
                acc.add(entry)
            }
            acc
        }
    }

    fun BarnetilsynBisysPeriode.hengerSammenMed(other: BarnetilsynBisysPeriode): Boolean {
        val tomNesteMåned = YearMonth.from(this.periode.tom.plusMonths(1))
        val otherFom = YearMonth.from(other.periode.fom)
        return tomNesteMåned == otherFom
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
        val sisteGjeldendeBehandling = behandlingService.finnSisteIverksatteBehandling(fagsak.id) ?: return null
        val startdato = tilkjentYtelseService.hentForBehandling(sisteGjeldendeBehandling.id).startdato

        val historikk = andelsHistorikkService.hentHistorikk(fagsak.id, null)
            .filter { it.erAktivVedtaksperiode() }
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
                }
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
                    barnIdenter = periode.barnIdenter
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

private fun Periode.union(periode: Periode): Periode {
    return Periode(fom = minOf(this.fom, periode.fom), tom = maxOf(this.tom, periode.tom))
}

private fun BarnetilsynBisysPeriode.harSammeBarn(entry: BarnetilsynBisysPeriode): Boolean {
    return barnIdenter.toSet() == entry.barnIdenter.toSet()
}
