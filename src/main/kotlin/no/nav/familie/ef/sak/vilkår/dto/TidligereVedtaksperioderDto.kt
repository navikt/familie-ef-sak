package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataPeriodeHistorikkOvergangsstønad
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType.SANKSJON
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

private fun List<GrunnlagsdataPeriodeHistorikkOvergangsstønad>.tilDto() = this.map { it.tilDto() }
    .slåSammenPåfølgendePerioderMedLikPeriodetype()
    .sortedByDescending { it.fom }

private fun GrunnlagsdataPeriodeHistorikkOvergangsstønad.tilDto() = GrunnlagsdataPeriodeHistorikkDto(
    periodeType = this.periodeType.name,
    fom = this.fom,
    tom = this.tom,
    antMnd = mndMedBeløp(periodeType, beløp, fom, tom),
    antallMndUtenBeløp = mndUtenBeløp(periodeType, beløp, fom, tom),
)

private fun mndUtenBeløp(
    periodeType: VedtaksperiodeType,
    beløp: Int,
    fom: LocalDate,
    tom: LocalDate,
) = when (beløp == 0 && periodeType != SANKSJON) {
    true -> Månedsperiode(fom, tom).lengdeIHeleMåneder()
    false -> 0
}

private fun mndMedBeløp(
    periodeType: VedtaksperiodeType,
    beløp: Int,
    fom: LocalDate,
    tom: LocalDate,
) = when (beløp != 0 || periodeType == SANKSJON) {
    true -> Månedsperiode(fom, tom).lengdeIHeleMåneder()
    false -> 0
}

fun List<GrunnlagsdataPeriodeHistorikkDto>.slåSammenPåfølgendePerioderMedLikPeriodetype(): List<GrunnlagsdataPeriodeHistorikkDto> {
    val sortertPåDatoListe = this.sortedBy { it.fom }
    return sortertPåDatoListe.fold(mutableListOf()) { returliste, dennePerioden ->
        when (skalSlåSammenForrigePeriodeMedDennePerioden(returliste, dennePerioden)) {
            true -> returliste.utvidSistePeriodeMed(dennePerioden)
            false -> returliste.add(dennePerioden)
        }
        returliste
    }
}

private fun MutableList<GrunnlagsdataPeriodeHistorikkDto>.utvidSistePeriodeMed(dennePerioden: GrunnlagsdataPeriodeHistorikkDto) {
    val sammeslått = slåSammenPeriodeHistorikkDto(this.last(), dennePerioden)
    this.byttUtSisteMed(sammeslått)
}

private fun <E> MutableList<E>.byttUtSisteMed(ny: E) {
    this.removeLast()
    this.add(ny)
}

private fun skalSlåSammenForrigePeriodeMedDennePerioden(
    liste: List<GrunnlagsdataPeriodeHistorikkDto>,
    denne: GrunnlagsdataPeriodeHistorikkDto,
) :Boolean {
    val forrige = liste.lastOrNull()
    return forrige != null && (forrige.periode() påfølgesAv denne.periode() && forrige.periodeType === denne.periodeType)
}



private fun slåSammenPeriodeHistorikkDto(
    forrige: GrunnlagsdataPeriodeHistorikkDto,
    denne: GrunnlagsdataPeriodeHistorikkDto,
) = GrunnlagsdataPeriodeHistorikkDto(
    periodeType = forrige.periodeType,
    fom = forrige.fom,
    tom = denne.tom,
    antMnd = forrige.antMnd + denne.antMnd,
    antallMndUtenBeløp = forrige.antallMndUtenBeløp + denne.antallMndUtenBeløp,
)

private fun GrunnlagsdataPeriodeHistorikkDto.periode(): Månedsperiode = Månedsperiode(this.fom, this.tom)
