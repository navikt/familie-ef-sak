package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AvslagÅrsak
import no.nav.familie.ef.sak.vedtak.domain.SamordningsfradragType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonert
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

object VedtakDtoUtil {

    fun innvilgelseOvergangsstønadDto() =
            InnvilgelseOvergangsstønad(
                    "periodebegrunnelse",
                    "inntektsbegrunnelse",
                    listOf(vedtaksperiodeDto()),
                    listOf(Inntekt(årMånedFra = YearMonth.of(2021, 1),
                                   forventetInntekt = BigDecimal(100_000),
                                   samordningsfradrag = BigDecimal(500))),
                    SamordningsfradragType.GJENLEVENDEPENSJON)

    fun innvilgelseBarnetilsynDto(barnId: UUID = UUID.randomUUID()) =
            InnvilgelseBarnetilsyn(
                    "begrunnelse",
                    listOf(barnetilsynperiodeDto(barnId)),
                    listOf(periodeMedBeløpDto()),
                    tilleggsstønadDto())

    fun avslagDto(årsak: AvslagÅrsak = AvslagÅrsak.BARN_OVER_ÅTTE_ÅR,
                  begrunnelse: String? = "en god begrunnelse") =
            Avslå(årsak, begrunnelse)

    fun opphørDto(fom: YearMonth = YearMonth.of(2022, 1),
                  begrunnelse: String? = "en god begrunnelse") =
            Opphør(fom, begrunnelse)

    fun sanksjonertDto(årsak: Sanksjonsårsak = Sanksjonsårsak.SAGT_OPP_STILLING,
                       periode: VedtaksperiodeDto = vedtaksperiodeDto(),
                       begrunnelse: String = "begrunnelse") =
            Sanksjonert(årsak, periode, begrunnelse)

    fun barnetilsynperiodeDto(barnId: UUID) =
            UtgiftsperiodeDto(
                    YearMonth.of(2021, 1),
                    YearMonth.of(2021, 12),
                    listOf(barnId),
                    BigDecimal(500))

    fun periodeMedBeløpDto() =
            PeriodeMedBeløpDto(
                    YearMonth.of(2021, 1),
                    YearMonth.of(2021, 12),
                    1000)

    fun tilleggsstønadDto() =
            TilleggsstønadDto(
                    true,
                    listOf(periodeMedBeløpDto()),
                    "begrunnelse tilleggstønad")

    fun vedtaksperiodeDto() =
            VedtaksperiodeDto(
                    YearMonth.of(2021, 1),
                    YearMonth.of(2021, 12),
                    AktivitetType.BARN_UNDER_ETT_ÅR,
                    VedtaksperiodeType.HOVEDPERIODE)

}