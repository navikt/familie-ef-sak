package no.nav.familie.ef.sak.ekstern.stønadsperiode

import no.nav.familie.ef.sak.ekstern.stønadsperiode.util.ArenaPeriodeUtil
import no.nav.familie.ef.sak.infotrygd.InternPeriode
import no.nav.familie.ef.sak.infotrygd.PeriodeService
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternPeriodeMedBeløp
import no.nav.familie.kontrakter.felles.ef.EksternPeriodeMedStønadstype
import no.nav.familie.kontrakter.felles.ef.EksternePerioderForStønadstyperRequest
import no.nav.familie.kontrakter.felles.ef.EksternePerioderMedStønadstypeResponse
import no.nav.familie.kontrakter.felles.ef.EksternePerioderRequest
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
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

    fun hentPerioderForOvergangsstønadOgSkolepenger(request: EksternePerioderRequest): EksternePerioderMedStønadstypeResponse {
        val perioderOS = periodeService.hentPerioderForOvergangsstønadFraEfOgInfotrygd(request.personIdent)
        val begrensetPeriode = Datoperiode(fom = request.fomDato ?: LocalDate.MIN, tom = request.tomDato ?: LocalDate.MAX)

        val eksternPeriodeMedStønadstypeOS =
            perioderOS
                .filter { periode ->
                    Datoperiode(fom = periode.stønadFom, tom = periode.stønadTom).overlapper(begrensetPeriode)
                }.map {
                    EksternPeriodeMedStønadstype(
                        it.stønadFom,
                        it.stønadTom,
                        StønadType.OVERGANGSSTØNAD,
                    )
                }

        val eksternPeriodeMedStønadstypeSP = periodeService.hentPeriodeFraVedtakForSkolepenger(request.personIdent)

        return EksternePerioderMedStønadstypeResponse(request.personIdent, eksternPeriodeMedStønadstypeOS + eksternPeriodeMedStønadstypeSP)
    }

    fun hentPerioderForYtelser(request: EksternePerioderForStønadstyperRequest): EksternePerioderMedStønadstypeResponse {
        val periodeFraRequest = Datoperiode(fom = request.fomDato ?: LocalDate.MIN, tom = request.tomDato ?: LocalDate.MAX)
        val stønadstyper =
            if (request.stønadstyper.isEmpty()) {
                listOf(StønadType.OVERGANGSSTØNAD, StønadType.BARNETILSYN, StønadType.SKOLEPENGER)
            } else {
                request.stønadstyper
            }

        val stønadPerioder =
            stønadstyper.flatMap { stønadType ->
                when (stønadType) {
                    StønadType.OVERGANGSSTØNAD -> {
                        periodeService
                            .hentPerioderForOvergangsstønadFraEfOgInfotrygd(request.personIdent)
                            .filtrerOverlappendePerioder(periodeFraRequest)
                            .mapToEksternPeriode(StønadType.OVERGANGSSTØNAD)
                    }
                    StønadType.BARNETILSYN -> {
                        periodeService
                            .hentPerioderForBarnetilsynFraEfOgInfotrygd(request.personIdent)
                            .filtrerOverlappendePerioder(periodeFraRequest)
                            .mapToEksternPeriode(StønadType.BARNETILSYN)
                    }
                    StønadType.SKOLEPENGER -> {
                        periodeService.hentPeriodeFraVedtakForSkolepenger(request.personIdent)
                    }
                }
            }

        return EksternePerioderMedStønadstypeResponse(request.personIdent, stønadPerioder)
    }

    private fun List<InternPeriode>.filtrerOverlappendePerioder(begrensetPeriode: Datoperiode): List<InternPeriode> =
        this.filter { periode ->
            Datoperiode(fom = periode.stønadFom, tom = periode.stønadTom).overlapper(begrensetPeriode)
        }

    private fun List<InternPeriode>.mapToEksternPeriode(stønadType: StønadType): List<EksternPeriodeMedStønadstype> =
        this.map {
            EksternPeriodeMedStønadstype(
                it.stønadFom,
                it.stønadTom,
                stønadType,
            )
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
