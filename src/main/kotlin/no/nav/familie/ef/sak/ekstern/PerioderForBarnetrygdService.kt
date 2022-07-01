package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.infotrygd.InternPeriode
import no.nav.familie.ef.sak.infotrygd.PeriodeService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad.Datakilde.EF
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad.Datakilde.INFOTRYGD
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.springframework.stereotype.Service

/**
 * Service som henter perioder fra EF sin base og infotrygd og slår disse sammen
 * Skal kun returnere de som har full overgangsstønad
 * Responsen brukes for å vurdere om personen skal få utvidet barnetrygd
 */
@Service
class PerioderForBarnetrygdService(
    private val periodeService: PeriodeService
) {

    fun hentPerioderMedFullOvergangsstønad(request: PersonIdent): PerioderOvergangsstønadResponse {
        val perioderPåDatakilde = periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(request.ident)
            .filter(InternPeriode::erFullOvergangsstønad)
            .map(InternPeriode::tilEksternPeriodeOvergangsstønad)
            .groupBy { it.datakilde }

        val perioderInfotrygd = infotrygdperioderUtenOverlapp(perioderPåDatakilde.getOrDefault(INFOTRYGD, emptyList()))
        val perioderEF = perioderPåDatakilde.getOrDefault(EF, emptyList())

        return PerioderOvergangsstønadResponse(perioderEF + perioderInfotrygd)
    }

    private fun infotrygdperioderUtenOverlapp(perioder: List<PeriodeOvergangsstønad>) =
        perioder
            .sortedWith(compareByDescending<PeriodeOvergangsstønad> { it.tomDato }.thenByDescending { it.fomDato })
            .fold<PeriodeOvergangsstønad, MutableList<PeriodeOvergangsstønad>>(mutableListOf()) { acc, gjeldende ->
                acc.fjernDuplikatOgSplittOverlappendePeriode(gjeldende)
            }
            .sortedByDescending { it.fomDato }
}

private fun PeriodeOvergangsstønad.tilPeriode(): Periode = Periode(this.fomDato, this.tomDato)
private fun PeriodeOvergangsstønad.omsluttesAv(annen: PeriodeOvergangsstønad): Boolean =
    this.tilPeriode().omsluttesAv(annen.tilPeriode())

private fun MutableList<PeriodeOvergangsstønad>.fjernDuplikatOgSplittOverlappendePeriode(gjeldende: PeriodeOvergangsstønad): MutableList<PeriodeOvergangsstønad> {
    val forrige = this.removeLastOrNull()
    when {
        forrige == null -> this.add(gjeldende)
        gjeldende.omsluttesAv(forrige) -> this.add(forrige)
        forrige.omsluttesAv(gjeldende) -> this.add(gjeldende)
        gjeldende.fomDato >= forrige.fomDato -> this.add(forrige) // I praksis omsluttes denne av forrige og tidligere perioder
        gjeldende.tomDato >= forrige.fomDato -> {
            this.add(forrige.copy(fomDato = maxOf(forrige.fomDato, gjeldende.fomDato)))
            this.add(
                gjeldende.copy(
                    fomDato = minOf(forrige.fomDato, gjeldende.fomDato),
                    tomDato = maxOf(forrige.fomDato, gjeldende.fomDato).minusDays(1)
                )
            )
        }
        else -> {
            this.add(forrige)
            this.add(gjeldende)
        }
    }
    return this
}

private fun InternPeriode.tilEksternPeriodeOvergangsstønad(): PeriodeOvergangsstønad =
    PeriodeOvergangsstønad(
        personIdent = this.personIdent,
        fomDato = this.stønadFom,
        tomDato = this.stønadTom,
        datakilde = this.datakilde
    )
