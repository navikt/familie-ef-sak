package no.nav.familie.ef.sak.api.simulering

import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseMedMetadata
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import java.math.BigDecimal
import java.time.LocalDate

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
    return TilkjentYtelseMedMetadata(tilkjentYtelse = this.tilIverksett(),
                                     saksbehandlerId = saksbehandlerId,
                                     eksternBehandlingId = eksternBehandlingId,
                                     stønadstype = StønadType.valueOf(stønadstype.name),
                                     eksternFagsakId = eksternFagsakId,
                                     personIdent = this.personident,
                                     behandlingId = this.behandlingId,
                                     vedtaksdato = this.vedtakstidspunkt?.toLocalDate() ?: LocalDate.now())
}

fun TilkjentYtelse.tilIverksett(): TilkjentYtelseDto {
    return TilkjentYtelseDto(andelerTilkjentYtelse = this.andelerTilkjentYtelse.map { it.tilIverksettDto() })
}