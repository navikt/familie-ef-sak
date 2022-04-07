package no.nav.familie.ef.sak.vedtak.dto

import no.nav.familie.ef.sak.beregning.barnetilsyn.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.beregning.barnetilsyn.UtgiftsperiodeDto
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.PeriodeMedBeløp
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import java.time.YearMonth

data class InnvilgelseBarnetilsyn(val begrunnelse: String?,
                                  val perioder: List<UtgiftsperiodeDto> = emptyList(),
                                  val perioderKontantstøtte: List<PeriodeMedBeløpDto>,
                                  val tilleggsstønad: TilleggsstønadDto) : VedtakDto(ResultatType.INNVILGE,
                                                                                     "InnvilgelseBarnetilsyn")

data class TilleggsstønadDto(val harTilleggsstønad: Boolean,
                             val perioder: List<PeriodeMedBeløpDto> = emptyList(),
                             val begrunnelse: String?)


fun UtgiftsperiodeDto.tilDomene(): Barnetilsynperiode =
        Barnetilsynperiode(datoFra = this.årMånedFra.atDay(1),
                           datoTil = this.årMånedTil.atEndOfMonth(),
                           utgifter = this.utgifter,
                           barn = this.barn)

fun PeriodeMedBeløpDto.tilDomene(): PeriodeMedBeløp =
        PeriodeMedBeløp(datoFra = this.årMånedFra.atDay(1),
                        datoTil = this.årMånedTil.atEndOfMonth(),
                        beløp = this.beløp)

fun Vedtak.mapInnvilgelseBarnetilsyn(): InnvilgelseBarnetilsyn {
    feilHvis(this.barnetilsyn == null || this.kontantstøtte == null || this.tilleggsstønad == null) {
        "Mangler felter fra vedtak for vedtak=${this.behandlingId}"
    }
    return InnvilgelseBarnetilsyn(
            begrunnelse = barnetilsyn.begrunnelse,
            perioder = barnetilsyn.perioder.map {
                UtgiftsperiodeDto(årMånedFra = YearMonth.from(it.datoFra),
                                  årMånedTil = YearMonth.from(it.datoTil),
                                  utgifter = it.utgifter,
                                  barn = it.barn)
            },
            perioderKontantstøtte = this.kontantstøtte.perioder.map { it.tilDto() },
            tilleggsstønad = TilleggsstønadDto(
                    harTilleggsstønad = this.tilleggsstønad.harTilleggsstønad,
                    perioder = this.tilleggsstønad.perioder.map { it.tilDto() },
                    begrunnelse = this.tilleggsstønad.begrunnelse
            )
    )
}