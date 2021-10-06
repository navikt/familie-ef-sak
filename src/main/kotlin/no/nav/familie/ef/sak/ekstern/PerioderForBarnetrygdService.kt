package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.infotrygd.InternPeriode
import no.nav.familie.ef.sak.infotrygd.PeriodeService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
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

    fun hentPerioder(request: PersonIdent): PerioderOvergangsstønadResponse {
        val perioder = periodeService.hentPerioderFraEfOgInfotrygd(request.ident)
                .filter(InternPeriode::erFullOvergangsstønad)
                .map { it.tilEksternPeriodeOvergangsstønad(request.ident) }

        return PerioderOvergangsstønadResponse(perioder)
    }

}

private fun InternPeriode.tilEksternPeriodeOvergangsstønad(personIdent: String): PeriodeOvergangsstønad =
        PeriodeOvergangsstønad(
                personIdent = personIdent,
                fomDato = this.stønadFom,
                tomDato = this.stønadTom,
                datakilde = this.datakilde
        )
