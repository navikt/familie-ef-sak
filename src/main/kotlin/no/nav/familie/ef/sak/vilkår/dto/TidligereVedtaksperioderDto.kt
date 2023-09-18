package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataPeriodeHistorikk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.LocalDate

data class TidligereVedtaksperioderDto(
    val infotrygd: TidligereInnvilgetVedtakDto?,
    val sak: TidligereInnvilgetVedtakDto?,
    val historiskPensjon: Boolean?,
) {
    fun harTidligereVedtaksperioder() =
        infotrygd?.harTidligereInnvilgetVedtak() ?: false || sak?.harTidligereInnvilgetVedtak() ?: false || historiskPensjon ?: true
}

data class TidligereInnvilgetVedtakDto(
    val harTidligereOvergangsstønad: Boolean,
    val harTidligereBarnetilsyn: Boolean,
    val harTidligereSkolepenger: Boolean,
    val periodeHistorikkOvergangsstønad: List<GrunnlagsdataPeriodeHistorikkDto> = emptyList(),
) {
    fun harTidligereInnvilgetVedtak() =
        harTidligereOvergangsstønad || harTidligereBarnetilsyn || harTidligereSkolepenger
}

data class GrunnlagsdataPeriodeHistorikkDto(
    val periodeType: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val antMnd: Long,
    val antallMndUtenBeløp: Long = 0,
)

fun TidligereVedtaksperioder?.tilDto(): TidligereVedtaksperioderDto = this?.let {
    TidligereVedtaksperioderDto(
        infotrygd = it.infotrygd.tilDto(),
        sak = it.sak?.tilDto(),
        historiskPensjon = it.historiskPensjon,
    )
} ?: TidligereVedtaksperioderDto(null, null, null)

fun TidligereInnvilgetVedtak.tilDto() =
    TidligereInnvilgetVedtakDto(
        harTidligereOvergangsstønad = this.harTidligereOvergangsstønad,
        harTidligereBarnetilsyn = this.harTidligereBarnetilsyn,
        harTidligereSkolepenger = this.harTidligereSkolepenger,
        periodeHistorikkOvergangsstønad = this.periodeHistorikkOvergangsstønad.tilDto(),
    )

private fun List<GrunnlagsdataPeriodeHistorikk>.tilDto() = this.map { it.tilDto() }
    .slåSammenPåfølgendePerioderMedLikPeriodetype()
    .sortedByDescending { it.fom }

private fun GrunnlagsdataPeriodeHistorikk.tilDto() = GrunnlagsdataPeriodeHistorikkDto(
    periodeType = this.periodeType.toString(),
    fom = this.fom,
    tom = this.tom,
    antMnd = mndMedBeløp(beløp, fom, tom),
    antallMndUtenBeløp = mndUtenBeløp(beløp, fom, tom),
)

private fun mndUtenBeløp(
    beløp: Int,
    fom: LocalDate,
    tom: LocalDate,
) = when (beløp == 0) {
    true -> Månedsperiode(fom, tom).lengdeIHeleMåneder()
    false -> 0
}

private fun mndMedBeløp(
    beløp: Int,
    fom: LocalDate,
    tom: LocalDate,
) = when (beløp == 0) {
    true -> 0
    false -> Månedsperiode(fom, tom).lengdeIHeleMåneder()
}

fun List<GrunnlagsdataPeriodeHistorikkDto>.slåSammenPåfølgendePerioderMedLikPeriodetype(): List<GrunnlagsdataPeriodeHistorikkDto> {
    val sortertPåDatoListe = this.sortedBy { it.fom }
    return sortertPåDatoListe.fold(mutableListOf()) { acc, entry ->
        val last = acc.lastOrNull()
        if (
            last != null && last.periode() påfølgesAv entry.periode() &&
            last.periodeType === entry.periodeType
        ) {
            acc.removeLast()
            val månedsperiode = last.periode() union entry.periode()
            acc.add(
                last.copy(
                    fom = månedsperiode.fomDato,
                    tom = månedsperiode.tomDato,
                    antMnd = last.antMnd + entry.antMnd,
                    antallMndUtenBeløp = last.antallMndUtenBeløp + entry.antallMndUtenBeløp,

                ),
            )
        } else {
            acc.add(entry)
        }
        acc
    }
}

private fun GrunnlagsdataPeriodeHistorikkDto.periode(): Månedsperiode = Månedsperiode(this.fom, this.tom)
