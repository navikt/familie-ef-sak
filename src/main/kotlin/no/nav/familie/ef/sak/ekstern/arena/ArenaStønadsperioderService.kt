package no.nav.familie.ef.sak.ekstern.arena

import no.nav.familie.ef.sak.infotrygd.PeriodeService
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadRequest
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import org.springframework.stereotype.Service

/**
 * Denne tjenesten henter perioder for andre typer stønader fra EF også
 * Arena kaller idag en endepunkt hos infotrygd som returnerer perioder fra alle stønader, men det var sagt i starten att de
 * skulle hente perioder for overgangsstønad fra oss.
 */
@Service
class ArenaStønadsperioderService(
        private val periodeService: PeriodeService
) {

    /**
     * Henter perioder fra infotrygd for en person
     * Skal hente perioder fra ef-sak også i fremtiden
     */
    fun hentPerioder(request: PerioderOvergangsstønadRequest): PerioderOvergangsstønadResponse {
        val perioderFraITogEF = slåSammenPerioderFraEfOgInfotrygd(request)
        return PerioderOvergangsstønadResponse(perioderFraITogEF)
    }

    private fun slåSammenPerioderFraEfOgInfotrygd(request: PerioderOvergangsstønadRequest): List<PeriodeOvergangsstønad> {
        val perioder = periodeService.hentPerioderFraEfOgInfotrygd(request.personIdent)
        return ArenaPeriodeUtil.slåSammenPerioderFraEfOgInfotrygd(request, perioder)
    }

}
