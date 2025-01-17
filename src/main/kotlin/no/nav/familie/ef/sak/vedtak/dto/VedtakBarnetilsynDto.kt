package no.nav.familie.ef.sak.vedtak.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.PeriodeMedBeløp
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.erSammenhengende
import java.time.YearMonth
import java.util.UUID

data class InnvilgelseBarnetilsyn(
    val begrunnelse: String?,
    val perioder: List<UtgiftsperiodeDto> = emptyList(),
    val perioderKontantstøtte: List<PeriodeMedBeløpDto>,
    val tilleggsstønad: TilleggsstønadDto,
    override val resultatType: ResultatType = ResultatType.INNVILGE,
    override val _type: String = "InnvilgelseBarnetilsyn",
) : VedtakDto(resultatType, _type)

data class TilleggsstønadDto(
    val harTilleggsstønad: Boolean?,
    val perioder: List<PeriodeMedBeløpDto> = emptyList(),
    val begrunnelse: String?,
)

data class PeriodeMedBeløpDto(
    @Deprecated("Bruk periode!", ReplaceWith("periode.fom")) val årMånedFra: YearMonth? = null,
    @Deprecated("Bruk periode!", ReplaceWith("periode.tom")) val årMånedTil: YearMonth? = null,
    @JsonIgnore
    val periode: Månedsperiode =
        Månedsperiode(
            årMånedFra ?: error("periode eller årMånedFra må ha verdi"),
            årMånedTil ?: error("periode eller årMånedTil må ha verdi"),
        ),
    val beløp: Int,
)

data class UtgiftsperiodeDto(
    @Deprecated("Bruk periode!", ReplaceWith("periode.fom")) val årMånedFra: YearMonth? = null,
    @Deprecated("Bruk periode!", ReplaceWith("periode.tom")) val årMånedTil: YearMonth? = null,
    @JsonIgnore
    val periode: Månedsperiode =
        Månedsperiode(
            årMånedFra ?: error("periode eller årMånedFra må ha verdi"),
            årMånedTil ?: error("periode eller årMånedTil må ha verdi"),
        ),
    val barn: List<UUID>,
    val utgifter: Int,
    val sanksjonsårsak: Sanksjonsårsak?,
    val periodetype: PeriodetypeBarnetilsyn,
    val aktivitetstype: AktivitetstypeBarnetilsyn?,
) {
    val erMidlertidigOpphørEllerSanksjon get() = periodetype.midlertidigOpphørEllerSanksjon()
}

fun List<UtgiftsperiodeDto>.tilPerioder(): List<Månedsperiode> = this.map(UtgiftsperiodeDto::periode)

fun List<UtgiftsperiodeDto>.erSammenhengende(): Boolean = map(UtgiftsperiodeDto::periode).erSammenhengende()

fun UtgiftsperiodeDto.tilDomene(): Barnetilsynperiode =
    Barnetilsynperiode(
        periode = this.periode,
        utgifter = this.utgifter,
        barn = this.barn,
        sanksjonsårsak = this.sanksjonsårsak,
        periodetype = this.periodetype,
        aktivitet = this.aktivitetstype,
    )

fun PeriodeMedBeløpDto.tilDomene(): PeriodeMedBeløp =
    PeriodeMedBeløp(
        periode = this.periode,
        beløp = this.beløp,
    )

fun Vedtak.mapInnvilgelseBarnetilsyn(resultatType: ResultatType = ResultatType.INNVILGE): InnvilgelseBarnetilsyn {
    feilHvis(this.barnetilsyn == null || this.kontantstøtte == null || this.tilleggsstønad == null) {
        "Mangler felter fra vedtak for vedtak=${this.behandlingId}"
    }
    return InnvilgelseBarnetilsyn(
        begrunnelse = barnetilsyn.begrunnelse,
        perioder =
            barnetilsyn.perioder.map {
                UtgiftsperiodeDto(
                    årMånedFra = YearMonth.from(it.datoFra),
                    årMånedTil = YearMonth.from(it.datoTil),
                    periode = it.periode,
                    utgifter = it.utgifter,
                    barn = it.barn,
                    sanksjonsårsak = it.sanksjonsårsak,
                    periodetype = it.periodetype,
                    aktivitetstype = it.aktivitetstype,
                )
            },
        perioderKontantstøtte = this.kontantstøtte.perioder.map { it.tilDto() },
        tilleggsstønad =
            TilleggsstønadDto(
                harTilleggsstønad = this.tilleggsstønad.harTilleggsstønad,
                perioder = this.tilleggsstønad.perioder.map { it.tilDto() },
                begrunnelse = this.tilleggsstønad.begrunnelse,
            ),
        resultatType = resultatType,
        _type =
            when (resultatType) {
                ResultatType.INNVILGE -> "InnvilgelseBarnetilsyn"
                ResultatType.INNVILGE_UTEN_UTBETALING -> "InnvilgelseBarnetilsynUtenUtbetaling"
                else -> error("Ugyldig resultattype $this")
            },
    )
}
