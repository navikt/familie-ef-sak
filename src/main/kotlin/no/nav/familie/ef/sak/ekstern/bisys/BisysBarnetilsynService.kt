package no.nav.familie.ef.sak.ekstern.bisys

import no.nav.familie.ef.sak.barn.BarnService
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
        private val barnService: BarnService,
        private val tilkjentYtelseService: TilkjentYtelseService,
        private val infotrygdService: InfotrygdService) {

    fun hentAllePerioder(personIdent: String, fomDato: LocalDate): BarnetilsynBisysResponse {
        return BarnetilsynBisysResponse(bisysPerioderEfOgInfotrygd(personIdent, fomDato))
    }

    private fun bisysPerioderEfOgInfotrygd(personIdent: String, fomDato: LocalDate): List<BarnetilsynBisysPeriode> {
        return (hentInfotrygdPerioderBarnetilsyn(personIdent, fomDato) + hentPerioderBarnetilsyn(personIdent, fomDato))
                .sortedBy { it.periode.fom }
    }

    private fun hentPerioderBarnetilsyn(personIdent: String, fomDato: LocalDate): List<BarnetilsynBisysPeriode> {

        val personIdenter = personService.hentPersonIdenter(personIdent).identer()
        val fagsak: Fagsak = fagsakService.finnFagsak(personIdenter, StønadType.BARNETILSYN)
                             ?: error("Kunne ikke finne fagsak for personident")

        val historikk = tilkjentYtelseService.hentHistorikk(fagsak.id, null)
                .filter { it.erIkkeFjernet() }
                .filter { it.andel.beløp > 0 && it.andel.stønadTil >= fomDato }

        val barnIdenter = historikk.flatMap { it.andel.barn }
                .distinct()
                .let { barnService.hentBehandlingBarnForBarnIder(it) }
                .associate { it.id to it.personIdent }

        val barnetilsynBisysPerioder = historikk.map { andel ->
            BarnetilsynBisysPeriode(Periode(andel.andel.stønadFra, andel.andel.stønadTil),
                                    andel.andel.barn.map {
                                        barnIdenter[it]
                                        ?: error("Fant ingen personident for barn=$it")
                                    },
                                    andel.andel.beløp,
                                    Datakilde.EF)
        }
        return barnetilsynBisysPerioder
    }

    private fun hentInfotrygdPerioderBarnetilsyn(personIdent: String, fomDato: LocalDate): List<BarnetilsynBisysPeriode> {
        val barnetilsynBisysPerioder =
                infotrygdService.hentPerioderFraReplika(personIdent).barnetilsyn.filter { it.stønadTom >= fomDato }
                        .map { periode ->
                            BarnetilsynBisysPeriode(Periode(periode.stønadFom, periode.stønadTom),
                                                    periode.barnIdenter,
                                                    periode.månedsbeløp,
                                                    Datakilde.INFOTRYGD)
                        }
        return barnetilsynBisysPerioder
    }
}