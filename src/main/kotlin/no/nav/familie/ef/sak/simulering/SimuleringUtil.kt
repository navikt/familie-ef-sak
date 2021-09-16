package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.fagsak.Stønadstype
import no.nav.familie.ef.sak.opplysninger.søknad.domain.TilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseMedMetadata
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import java.math.BigDecimal
import java.time.LocalDate

fun tilSimuleringsresultatDto(detaljertSimuleringResultat: DetaljertSimuleringResultat, tidSimuleringHentet: LocalDate): SimuleringsresultatDto {
    val perioder = grupperPosteringerEtterDato(detaljertSimuleringResultat.simuleringMottaker)

    val framtidigePerioder =
            perioder.filter {
                it.fom > tidSimuleringHentet ||
                (it.tom > tidSimuleringHentet && it.forfallsdato > tidSimuleringHentet)
            }

    val nestePeriode = framtidigePerioder.filter { it.feilutbetaling == BigDecimal.ZERO }.minByOrNull { it.fom }
    val tomSisteUtbetaling = perioder.filter { nestePeriode == null || it.fom < nestePeriode.fom }.maxOfOrNull { it.tom }

    return SimuleringsresultatDto(
            perioder = perioder,
            fomDatoNestePeriode = nestePeriode?.fom,
            etterbetaling = hentTotalEtterbetaling(perioder, nestePeriode?.fom),
            feilutbetaling = hentTotalFeilutbetaling(perioder, nestePeriode?.fom),
            fom = perioder.minOfOrNull { it.fom },
            tomDatoNestePeriode = nestePeriode?.tom,
            forfallsdatoNestePeriode = nestePeriode?.forfallsdato,
            tidSimuleringHentet = tidSimuleringHentet,
            tomSisteUtbetaling = tomSisteUtbetaling,
    )
}

private fun grupperPosteringerEtterDato(mottakere: List<SimuleringMottaker>): List<SimuleringsPeriode> {
    val simuleringPerioder = mutableMapOf<LocalDate, MutableList<SimulertPostering>>()


    mottakere.forEach {
        it.simulertPostering.filter { it.posteringType == PosteringType.YTELSE || it.posteringType == PosteringType.FEILUTBETALING }
                .forEach { postering ->
                    if (simuleringPerioder.containsKey(postering.fom))
                        simuleringPerioder[postering.fom]?.add(postering)
                    else simuleringPerioder[postering.fom] = mutableListOf(postering)
                }
    }

    return simuleringPerioder.map { (fom, posteringListe) ->
        SimuleringsPeriode(
                fom,
                posteringListe[0].tom,
                posteringListe[0].forfallsdato,
                nyttBeløp = hentNyttBeløpIPeriode(posteringListe),
                tidligereUtbetalt = hentTidligereUtbetaltIPeriode(posteringListe),
                resultat = hentResultatIPeriode(posteringListe),
                feilutbetaling = hentFeilbetalingIPeriode(posteringListe),
        )
    }
}


fun hentNyttBeløpIPeriode(periode: List<SimulertPostering>): BigDecimal {
    val sumPositiveYtelser = periode.filter { postering ->
        postering.posteringType == PosteringType.YTELSE && postering.beløp > BigDecimal.ZERO
    }.sumOf { it.beløp }
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling > BigDecimal.ZERO) sumPositiveYtelser - feilutbetaling else sumPositiveYtelser
}

fun hentFeilbetalingIPeriode(periode: List<SimulertPostering>) =
        periode.filter { postering ->
            postering.posteringType == PosteringType.FEILUTBETALING
        }.sumOf { it.beløp }

fun hentTidligereUtbetaltIPeriode(periode: List<SimulertPostering>): BigDecimal {
    val sumNegativeYtelser = periode.filter { postering ->
        (postering.posteringType === PosteringType.YTELSE && postering.beløp < BigDecimal.ZERO)
    }.sumOf { -it.beløp }
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling < BigDecimal.ZERO) sumNegativeYtelser - feilutbetaling else sumNegativeYtelser
}

fun hentResultatIPeriode(periode: List<SimulertPostering>) =
        if (periode.map { it.posteringType }.contains(PosteringType.FEILUTBETALING)) {
            periode.filter {
                it.posteringType == PosteringType.FEILUTBETALING
            }.sumOf { -it.beløp }
        } else
            periode.sumOf { it.beløp }

fun hentTotalEtterbetaling(simuleringPerioder: List<SimuleringsPeriode>, fomDatoNestePeriode: LocalDate?) =
        simuleringPerioder.filter {
            it.resultat > BigDecimal.ZERO && (fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode)
        }.sumOf { it.resultat }


fun hentTotalFeilutbetaling(simuleringPerioder: List<SimuleringsPeriode>, fomDatoNestePeriode: LocalDate?) =
        simuleringPerioder.filter { fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode }.sumOf { it.feilutbetaling }

fun TilkjentYtelse.tilIverksettMedMetaData(saksbehandlerId: String,
                                           eksternBehandlingId: Long,
                                           stønadstype: Stønadstype,
                                           eksternFagsakId: Long): TilkjentYtelseMedMetadata {
    return TilkjentYtelseMedMetadata(tilkjentYtelse = this.tilIverksettDto(),
                                     saksbehandlerId = saksbehandlerId,
                                     eksternBehandlingId = eksternBehandlingId,
                                     stønadstype = StønadType.valueOf(stønadstype.name),
                                     eksternFagsakId = eksternFagsakId,
                                     personIdent = this.personident,
                                     behandlingId = this.behandlingId,
                                     vedtaksdato = this.vedtakstidspunkt.toLocalDate())
}

