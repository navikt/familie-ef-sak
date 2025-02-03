package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.SamordningsfradragType
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.DelårsperiodeSkoleårDto
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseSkolepenger
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.OpphørSkolepenger
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonert
import no.nav.familie.ef.sak.vedtak.dto.SanksjonertPeriodeDto
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vedtak.dto.SkolepengerUtgiftDto
import no.nav.familie.ef.sak.vedtak.dto.SkoleårsperiodeSkolepengerDto
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.kontrakter.ef.felles.AvslagÅrsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

object VedtakDtoUtil {
    fun innvilgelseOvergangsstønadDto() =
        InnvilgelseOvergangsstønad(
            "periodebegrunnelse",
            "inntektsbegrunnelse",
            listOf(vedtaksperiodeDto()),
            listOf(
                Inntekt(
                    årMånedFra = YearMonth.of(2021, 1),
                    dagsats = BigDecimal.ZERO,
                    månedsinntekt = BigDecimal.ZERO,
                    forventetInntekt = BigDecimal(100_000),
                    samordningsfradrag = BigDecimal(500),
                ),
            ),
            SamordningsfradragType.GJENLEVENDEPENSJON,
        )

    fun innvilgelseBarnetilsynDto(
        barnId: UUID = UUID.randomUUID(),
        kontantstøtteBegrunnelse: String? = null,
    ) = InnvilgelseBarnetilsyn(
        begrunnelse = "begrunnelse",
        perioder =
            listOf(
                barnetilsynperiodeDto(
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2021, 12),
                    barn = listOf(barnId),
                ),
            ),
        perioderKontantstøtte = listOf(periodeMedBeløpDto()),
        kontantstøtteBegrunnelse = kontantstøtteBegrunnelse,
        tilleggsstønad = tilleggsstønadDto(),
    )

    fun innvilgelseSkolepengerDto() =
        InnvilgelseSkolepenger(
            "begrunnelse",
            listOf(skolepengerperiodeDto()),
        )

    fun opphørSkolepengerDto() =
        OpphørSkolepenger(
            "begrunnelse",
            listOf(skolepengerperiodeDto()),
        )

    fun avslagDto(
        årsak: AvslagÅrsak = AvslagÅrsak.BARN_OVER_ÅTTE_ÅR,
        begrunnelse: String? = "en god begrunnelse",
    ) = Avslå(årsak, begrunnelse)

    fun opphørDto(
        fom: YearMonth = YearMonth.of(2022, 1),
        begrunnelse: String? = "en god begrunnelse",
    ) = Opphør(fom, begrunnelse)

    fun sanksjonertDto(
        årsak: Sanksjonsårsak = Sanksjonsårsak.SAGT_OPP_STILLING,
        periode: SanksjonertPeriodeDto = sanksjonertPeriodeDto(),
        begrunnelse: String = "begrunnelse",
    ) = Sanksjonert(årsak, periode, begrunnelse)

    fun barnetilsynperiodeDto(
        fom: YearMonth,
        tom: YearMonth,
        barn: List<UUID>,
        utgifter: Int = 500,
        periodetype: PeriodetypeBarnetilsyn = PeriodetypeBarnetilsyn.ORDINÆR,
        aktivitetType: AktivitetstypeBarnetilsyn? = if (periodetype == PeriodetypeBarnetilsyn.ORDINÆR) AktivitetstypeBarnetilsyn.I_ARBEID else null,
        sanksjonsårsak: Sanksjonsårsak? = null,
    ) = UtgiftsperiodeDto(
        årMånedFra = fom,
        årMånedTil = tom,
        periode = Månedsperiode(fom, tom),
        barn = barn,
        utgifter = utgifter,
        sanksjonsårsak = sanksjonsårsak,
        periodetype = periodetype,
        aktivitetstype = aktivitetType,
    )

    fun periodeMedBeløpDto() =
        PeriodeMedBeløpDto(
            YearMonth.of(2021, 1),
            YearMonth.of(2021, 12),
            Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 12)),
            1000,
        )

    fun tilleggsstønadDto() =
        TilleggsstønadDto(
            true,
            listOf(periodeMedBeløpDto()),
            "begrunnelse tilleggstønad",
        )

    fun vedtaksperiodeDto() =
        VedtaksperiodeDto(
            YearMonth.of(2021, 1),
            YearMonth.of(2021, 12),
            Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 12)),
            AktivitetType.BARN_UNDER_ETT_ÅR,
            VedtaksperiodeType.HOVEDPERIODE,
        )

    fun skolepengerperiodeDto() =
        SkoleårsperiodeSkolepengerDto(
            perioder = listOf(skolepengerDelårsperiode()),
            utgiftsperioder =
                listOf(
                    SkolepengerUtgiftDto(
                        id = UUID.fromString("c076a0b9-0eb9-4a1b-bdcb-d75ebc40570d"),
                        årMånedFra = YearMonth.of(2021, 1),
                        stønad = 500,
                    ),
                ),
        )

    fun skolepengerDelårsperiode() =
        DelårsperiodeSkoleårDto(
            SkolepengerStudietype.HØGSKOLE_UNIVERSITET,
            YearMonth.of(2021, 1),
            YearMonth.of(2021, 12),
            Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 12)),
            50,
        )

    fun sanksjonertPeriodeDto() =
        SanksjonertPeriodeDto(
            YearMonth.of(2021, 1),
            YearMonth.of(2021, 1),
            YearMonth.of(2021, 1),
            YearMonth.of(2021, 1),
        )
}
