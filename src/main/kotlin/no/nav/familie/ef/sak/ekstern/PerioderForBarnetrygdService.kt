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

    fun hentPerioderMedFullOvergangsstønad(request: PersonIdent): PerioderOvergangsstønadResponse {
        val perioder = periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(request.ident)
                .filter(InternPeriode::erFullOvergangsstønad)
                .map(InternPeriode::tilEksternPeriodeOvergangsstønad)

        return PerioderOvergangsstønadResponse(perioder)
    }

}

private fun InternPeriode.tilEksternPeriodeOvergangsstønad(): PeriodeOvergangsstønad =
        PeriodeOvergangsstønad(
                personIdent = this.personIdent,
                fomDato = this.stønadFom,
                tomDato = this.stønadTom,
                datakilde = this.datakilde
        )
