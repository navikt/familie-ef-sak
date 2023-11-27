package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataPeriodeHistorikkBarnetilsyn
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
    val periodeHistorikkBarnetilsyn: List<GrunnlagsdataPeriodeHistorikkBarnetilsynDto> = emptyList(),
) {
    fun harTidligereInnvilgetVedtak() =
        harTidligereOvergangsstønad || harTidligereBarnetilsyn || harTidligereSkolepenger
}

data class GrunnlagsdataPeriodeHistorikkDto(
    val vedtaksperiodeType: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val antallMåneder: Long,
    val antallMånederUtenBeløp: Long = 0,
)

data class GrunnlagsdataPeriodeHistorikkBarnetilsynDto(
    val fom: LocalDate,
    val tom: LocalDate,
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
        periodeHistorikkOvergangsstønad = this.periodeHistorikkOvergangsstønad.tilDtoOvergangsstønad(),
        periodeHistorikkBarnetilsyn = this.periodeHistorikkBarnetilsyn.tilDtoBarnetilsyn(),
    )

private fun List<GrunnlagsdataPeriodeHistorikkOvergangsstønad>.tilDtoOvergangsstønad() = this.map { it.tilDto() }
    .slåSammenPåfølgendePerioderMedLikPeriodetype()
    .sortedByDescending { it.fom }

private fun List<GrunnlagsdataPeriodeHistorikkBarnetilsynDto>.slåSammenHistoriskePerioder(): List<GrunnlagsdataPeriodeHistorikkBarnetilsynDto> {
    val sortertePerioder = this.sortedByDescending { it.fom }
    return sortertePerioder.fold(mutableListOf()) { resultat, periode ->
        if (resultat.isNotEmpty() && resultat.last().periode() påfølgesAv periode.periode()) {
            val siste = resultat.removeLast()
            resultat.add(GrunnlagsdataPeriodeHistorikkBarnetilsynDto(siste.fom, periode.tom))
        } else {
            resultat.add(periode)
        }
        resultat
    }

}

private fun List<GrunnlagsdataPeriodeHistorikkBarnetilsyn>.tilDtoBarnetilsyn() = this.map { it.tilDto() }
    .slåSammenHistoriskePerioder()

private fun GrunnlagsdataPeriodeHistorikkOvergangsstønad.tilDto() = GrunnlagsdataPeriodeHistorikkDto(
    vedtaksperiodeType = this.periodeType.name,
    fom = this.fom,
    tom = this.tom,
    antallMåneder = månederMedBeløp(periodeType, beløp, fom, tom),
    antallMånederUtenBeløp = månederUtenBeløp(periodeType, beløp, fom, tom),
)

private fun GrunnlagsdataPeriodeHistorikkBarnetilsyn.tilDto() = GrunnlagsdataPeriodeHistorikkBarnetilsynDto(
    fom = this.fom,
    tom = this.tom,
)

private fun månederUtenBeløp(
    periodeType: VedtaksperiodeType,
    beløp: Int,
    fom: LocalDate,
    tom: LocalDate,
) = when (beløp == 0 && periodeType != SANKSJON) {
    true -> Månedsperiode(fom, tom).lengdeIHeleMåneder()
    false -> 0
}

private fun månederMedBeløp(
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
): Boolean {
    val forrige = liste.lastOrNull()
    return forrige != null && (forrige.periode() påfølgesAv denne.periode() && forrige.vedtaksperiodeType === denne.vedtaksperiodeType)
}

private fun slåSammenPeriodeHistorikkDto(
    forrige: GrunnlagsdataPeriodeHistorikkDto,
    denne: GrunnlagsdataPeriodeHistorikkDto,
) = GrunnlagsdataPeriodeHistorikkDto(
    vedtaksperiodeType = forrige.vedtaksperiodeType,
    fom = forrige.fom,
    tom = denne.tom,
    antallMåneder = forrige.antallMåneder + denne.antallMåneder,
    antallMånederUtenBeløp = forrige.antallMånederUtenBeløp + denne.antallMånederUtenBeløp,
)

private fun GrunnlagsdataPeriodeHistorikkDto.periode(): Månedsperiode = Månedsperiode(this.fom, this.tom)
private fun GrunnlagsdataPeriodeHistorikkBarnetilsynDto.periode(): Månedsperiode = Månedsperiode(this.fom, this.tom)
