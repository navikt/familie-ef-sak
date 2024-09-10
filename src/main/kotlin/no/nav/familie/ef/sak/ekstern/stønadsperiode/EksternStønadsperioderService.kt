package no.nav.familie.ef.sak.ekstern.stønadsperiode

import no.nav.familie.ef.sak.ekstern.stønadsperiode.util.ArenaPeriodeUtil
import no.nav.familie.ef.sak.infotrygd.InternPeriode
import no.nav.familie.ef.sak.infotrygd.PeriodeService
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternPeriodeMedBeløp
import no.nav.familie.kontrakter.felles.ef.EksternPeriodeMedStønadstype
import no.nav.familie.kontrakter.felles.ef.EksternePerioderRequest
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import no.nav.familie.kontrakter.felles.ef.OvergangsstønadOgSkolepengerResponse
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EksternStønadsperioderService(
    private val periodeService: PeriodeService,
) {
    /**
     * Brukes av arena: OBS: fom/tom-dato begrenses til DAGENS dato dersom det ikke er satt!
     */
    fun hentPerioderForAlleStønader(request: EksternePerioderRequest): EksternePerioderResponse {
        val perioder = periodeService.hentPerioderFraEfOgInfotrygd(request.personIdent)
        val perioderFraITogEF = ArenaPeriodeUtil.slåSammenPerioderFraEfOgInfotrygd(request, perioder)
        return EksternePerioderResponse(perioderFraITogEF)
    }

    fun hentPerioderForOvergangsstønadOgSkolepenger(request: EksternePerioderRequest): OvergangsstønadOgSkolepengerResponse {
        val perioderOS = periodeService.hentPerioderFraEf(setOf(request.personIdent), StønadType.OVERGANGSSTØNAD)
        val perioderSP = periodeService.hentPerioderFraEf(setOf(request.personIdent), StønadType.SKOLEPENGER)

        val eksternPeriodeMedStønadstypeOS =
            perioderOS?.internperioder?.map {
                EksternPeriodeMedStønadstype(
                    it.stønadFom,
                    it.stønadTom,
                    StønadType.OVERGANGSSTØNAD,
                )
            } ?: emptyList()

        val eksternPeriodeMedStønadstypeSP =
            perioderSP?.internperioder?.map {
                EksternPeriodeMedStønadstype(
                    it.stønadFom,
                    it.stønadTom,
                    StønadType.SKOLEPENGER,
                )
            } ?: emptyList()

        return OvergangsstønadOgSkolepengerResponse(request.personIdent, eksternPeriodeMedStønadstypeOS + eksternPeriodeMedStønadstypeSP)
    }

    fun hentPerioderForOvergangsstønad(request: EksternePerioderRequest): List<EksternPeriode> =
        hentPerioderForOvergangsstønadMedBeløp(request).map {
            EksternPeriode(
                personIdent = request.personIdent,
                fomDato = it.fomDato,
                tomDato = it.tomDato,
                datakilde = it.datakilde,
            )
        }

    fun hentPerioderForOvergangsstønadMedBeløp(request: EksternePerioderRequest): List<EksternPeriodeMedBeløp> =
        hentPerioderForOvergangsstønadAvgrensetPeriode(
            personIdent = request.personIdent,
            fomDato = request.fomDato,
            tomDato = request.tomDato,
        ).map {
            EksternPeriodeMedBeløp(
                personIdent = request.personIdent,
                fomDato = it.stønadFom,
                tomDato = it.stønadTom,
                datakilde = it.datakilde,
                beløp = it.månedsbeløp,
            )
        }

    private fun hentPerioderForOvergangsstønadAvgrensetPeriode(
        personIdent: String,
        fomDato: LocalDate?,
        tomDato: LocalDate?,
    ): List<InternPeriode> {
        val perioder = periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(personIdent)
        val begrensetPeriode = Datoperiode(fom = fomDato ?: LocalDate.MIN, tom = tomDato ?: LocalDate.MAX)
        return perioder
            .filter { periode ->
                Datoperiode(fom = periode.stønadFom, tom = periode.stønadTom).overlapper(begrensetPeriode)
            }
    }
}
