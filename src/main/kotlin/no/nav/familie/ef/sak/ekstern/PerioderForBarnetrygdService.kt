package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.infotrygd.InternPeriode
import no.nav.familie.ef.sak.infotrygd.PeriodeService
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import org.springframework.stereotype.Service

/**
 * Service som henter perioder fra EF sin base og infotrygd og slår disse sammen
 * Skal kun returnere de som har full overgangsstønad
 * Responsen brukes for å vurdere om personen skal få utvidet barnetrygd
 */
@Service
class PerioderForBarnetrygdService(
    private val periodeService: PeriodeService,
) {
    fun hentPerioderMedFullOvergangsstønad(request: PersonIdent): EksternePerioderResponse {
        val perioderPåDatakilde =
            periodeService
                .hentPerioderForOvergangsstønadFraEfOgInfotrygd(request.ident)
                .filter(InternPeriode::erFullOvergangsstønad)
                .map(InternPeriode::tilEksternEksternPeriode)
                .groupBy { it.datakilde }

        val perioderInfotrygd = infotrygdperioderUtenOverlapp(perioderPåDatakilde.getOrDefault(Datakilde.INFOTRYGD, emptyList()))
        val perioderEF = perioderPåDatakilde.getOrDefault(Datakilde.EF, emptyList())

        return EksternePerioderResponse(perioderEF + perioderInfotrygd)
    }

    private fun infotrygdperioderUtenOverlapp(perioder: List<EksternPeriode>) =
        perioder
            .sortedWith(compareByDescending<EksternPeriode> { it.tomDato }.thenByDescending { it.fomDato })
            .fold(mutableListOf<EksternPeriode>()) { acc, gjeldende ->
                acc.fjernDuplikatOgSplittOverlappendePeriode(gjeldende)
            }.sortedByDescending { it.fomDato }
}

private fun EksternPeriode.tilPeriode(): Datoperiode = Datoperiode(this.fomDato, this.tomDato)

private fun EksternPeriode.omsluttesAv(annen: EksternPeriode): Boolean = this.tilPeriode().omsluttesAv(annen.tilPeriode())

private fun MutableList<EksternPeriode>.fjernDuplikatOgSplittOverlappendePeriode(gjeldende: EksternPeriode): MutableList<EksternPeriode> {
    val forrige = this.removeLastOrNull()
    when {
        forrige == null -> {
            this.add(gjeldende)
        }

        gjeldende.omsluttesAv(forrige) -> {
            this.add(forrige)
        }

        forrige.omsluttesAv(gjeldende) -> {
            this.add(gjeldende)
        }

        gjeldende.fomDato >= forrige.fomDato -> {
            this.add(forrige)
        }

        // I praksis omsluttes denne av forrige og tidligere perioder
        gjeldende.tomDato >= forrige.fomDato -> {
            this.add(forrige.copy(fomDato = maxOf(forrige.fomDato, gjeldende.fomDato)))
            this.add(
                gjeldende.copy(
                    fomDato = minOf(forrige.fomDato, gjeldende.fomDato),
                    tomDato = maxOf(forrige.fomDato, gjeldende.fomDato).minusDays(1),
                ),
            )
        }

        else -> {
            this.add(forrige)
            this.add(gjeldende)
        }
    }
    return this
}

private fun InternPeriode.tilEksternEksternPeriode(): EksternPeriode =
    EksternPeriode(
        personIdent = this.personIdent,
        fomDato = this.stønadFom,
        tomDato = this.stønadTom,
        datakilde = this.datakilde,
    )
