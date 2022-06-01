package no.nav.familie.ef.sak.vedtak.dto

import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.felles.util.erPåfølgende
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.PeriodeMedBeløp
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import java.time.YearMonth
import java.util.UUID

data class InnvilgelseBarnetilsyn(
    val begrunnelse: String?,
    val perioder: List<UtgiftsperiodeDto> = emptyList(),
    val perioderKontantstøtte: List<PeriodeMedBeløpDto>,
    val tilleggsstønad: TilleggsstønadDto,
    override val resultatType: ResultatType = ResultatType.INNVILGE,
    override val _type: String = "InnvilgelseBarnetilsyn"
) : VedtakDto(resultatType, _type)

data class TilleggsstønadDto(
    val harTilleggsstønad: Boolean,
    val perioder: List<PeriodeMedBeløpDto> = emptyList(),
    val begrunnelse: String?
)

data class PeriodeMedBeløpDto(
    val årMånedFra: YearMonth,
    val årMånedTil: YearMonth,
    val beløp: Int

) {

    fun tilPeriode(): Periode = Periode(this.årMånedFra.atDay(1), this.årMånedTil.atEndOfMonth())
}

data class UtgiftsperiodeDto(
        val årMånedFra: YearMonth,
        val årMånedTil: YearMonth,
        val barn: List<UUID>,
        val utgifter: Int,
        val erMidlertidigOpphør: Boolean
) {

    fun tilPeriode(): Periode = Periode(this.årMånedFra.atDay(1), this.årMånedTil.atEndOfMonth())
}

fun List<UtgiftsperiodeDto>.tilPerioder(): List<Periode> =
    this.map {
        it.tilPeriode()
    }

fun List<UtgiftsperiodeDto>.midlertidigOpphørErSammenhengende(): Boolean = this.foldIndexed(true) { index, acc, periode ->
    if (index == 0) {
        acc
    } else {
        if (periode.erMidlertidigOpphør) {
            val forrigePeriode = this[index - 1]
            when {
                forrigePeriode.årMånedTil.erPåfølgende(periode.årMånedFra) -> acc
                else -> false
            }
        } else acc
    }
}

fun UtgiftsperiodeDto.tilDomene(): Barnetilsynperiode =
        Barnetilsynperiode(datoFra = this.årMånedFra.atDay(1),
                           datoTil = this.årMånedTil.atEndOfMonth(),
                           utgifter = this.utgifter,
                           barn = this.barn,
                           erMidlertidigOpphør = this.erMidlertidigOpphør)

fun PeriodeMedBeløpDto.tilDomene(): PeriodeMedBeløp =
    PeriodeMedBeløp(
        datoFra = this.årMånedFra.atDay(1),
        datoTil = this.årMånedTil.atEndOfMonth(),
        beløp = this.beløp
    )

fun Vedtak.mapInnvilgelseBarnetilsyn(resultatType: ResultatType = ResultatType.INNVILGE): InnvilgelseBarnetilsyn {
    feilHvis(this.barnetilsyn == null || this.kontantstøtte == null || this.tilleggsstønad == null) {
        "Mangler felter fra vedtak for vedtak=${this.behandlingId}"
    }
    return InnvilgelseBarnetilsyn(
        begrunnelse = barnetilsyn.begrunnelse,
        perioder = barnetilsyn.perioder.map {
            UtgiftsperiodeDto(
                årMånedFra = YearMonth.from(it.datoFra),
                årMånedTil = YearMonth.from(it.datoTil),
                utgifter = it.utgifter.toInt(),
                barn = it.barn,
                erMidlertidigOpphør = it.erMidlertidigOpphør?: false
            )
        },
        perioderKontantstøtte = this.kontantstøtte.perioder.map { it.tilDto() },
        tilleggsstønad = TilleggsstønadDto(
            harTilleggsstønad = this.tilleggsstønad.harTilleggsstønad,
            perioder = this.tilleggsstønad.perioder.map { it.tilDto() },
            begrunnelse = this.tilleggsstønad.begrunnelse
        ),
        resultatType = resultatType,
        _type = when (resultatType) {
            ResultatType.INNVILGE -> "InnvilgelseBarnetilsyn"
            ResultatType.INNVILGE_UTEN_UTBETALING -> "InnvilgelseBarnetilsynUtenUtbetaling"
            else -> error("Ugyldig resultattype $this")
        }
    )
}

fun Barnetilsynperiode.fraDomeneForSanksjon(): SanksjonertPeriodeDto =
    SanksjonertPeriodeDto(
        årMånedFra = YearMonth.from(datoFra),
        årMånedTil = YearMonth.from(datoTil)
    )
