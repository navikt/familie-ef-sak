package no.nav.familie.ef.sak.vedtak.dto

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.domain.DelårsperiodeSkoleårSkolepenger
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerUtgift
import no.nav.familie.ef.sak.vedtak.domain.SkoleårsperiodeSkolepenger
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import java.time.YearMonth

data class InnvilgelseSkolepenger(
    val begrunnelse: String?,
    val skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>
) :
    VedtakDto(resultatType = ResultatType.INNVILGE, _type = "InnvilgelseSkolepenger")

data class SkoleårsperiodeSkolepengerDto(
    val perioder: List<DelårsperiodeSkoleårDto>,
    val utgifter: List<SkolepengerUtgiftDto>
)

data class DelårsperiodeSkoleårDto(
    val studietype: SkolepengerStudietype, // TODO valider alle er like
    val årMånedFra: YearMonth,
    val årMånedTil: YearMonth,
    val studiebelastning: Int,
)

data class SkolepengerUtgiftDto(
    val årMånedFra: YearMonth,
    val utgifter: Int,
    val stønad: Int
)

fun SkoleårsperiodeSkolepengerDto.tilDomene() = SkoleårsperiodeSkolepenger(
    perioder = this.perioder.map { it.tilDomene() },
    utgifter = this.utgifter.map { SkolepengerUtgift(
        årMånedFra = it.årMånedFra,
        utgifter = it.utgifter,
        stønad = it.stønad
    ) }
)

fun DelårsperiodeSkoleårDto.tilDomene() = DelårsperiodeSkoleårSkolepenger(
    studietype = this.studietype,
    datoFra = this.årMånedFra.atDay(1),
    datoTil = this.årMånedTil.atEndOfMonth(),
    studiebelastning = this.studiebelastning
)

fun Vedtak.mapInnvilgelseSkolepenger(): InnvilgelseSkolepenger {
    feilHvis(this.skolepenger == null) {
        "Mangler felter fra vedtak for vedtak=${this.behandlingId}"
    }
    return InnvilgelseSkolepenger(
        begrunnelse = this.skolepenger.begrunnelse,
        skoleårsperioder = this.skolepenger.skoleårsperioder.map { skoleår ->
            SkoleårsperiodeSkolepengerDto(
                perioder = skoleår.perioder.map { it.tilDto() },
                utgifter = skoleår.utgifter.map { it.tilDto() }
            )
        }
    )
}

fun DelårsperiodeSkoleårSkolepenger.tilDto() = DelårsperiodeSkoleårDto(
    studietype = this.studietype,
    årMånedFra = YearMonth.from(this.datoFra),
    årMånedTil = YearMonth.from(this.datoTil),
    studiebelastning = this.studiebelastning
)

fun SkolepengerUtgift.tilDto() = SkolepengerUtgiftDto(
    årMånedFra = this.årMånedFra,
    utgifter = this.utgifter,
    stønad = this.stønad
)

fun SkoleårsperiodeSkolepengerDto.fraDomeneForSanksjon(): SanksjonertPeriodeDto =
    SanksjonertPeriodeDto(
        årMånedFra = YearMonth.from(this.perioder.single().årMånedFra),
        årMånedTil = YearMonth.from(this.perioder.single().årMånedTil)
    )
