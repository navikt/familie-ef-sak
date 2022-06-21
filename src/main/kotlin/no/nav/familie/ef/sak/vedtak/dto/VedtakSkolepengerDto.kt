package no.nav.familie.ef.sak.vedtak.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.felles.util.Skoleår
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.domain.DelårsperiodeSkoleårSkolepenger
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerUtgift
import no.nav.familie.ef.sak.vedtak.domain.SkoleårsperiodeSkolepenger
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import java.time.YearMonth
import java.util.UUID

/**
 * Innvilgelse og opphør blir nesten behandlet likt for skolepenger.
 * De begge oppdaterer Vedtak med totaltbilde for et state.
 * Innvilgelse tillaterer ikke sletting av perioder eller sletting/endring av utgifter
 * De har ulike resultattyper for at man skal vite hvilken typ av hendelse det er
 */
sealed class VedtakSkolepengerDto(
    resultatType: ResultatType,
    _type: String
) : VedtakDto(resultatType, _type) {
    abstract val begrunnelse: String?
    abstract val skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>

    fun erOpphør() = this is OpphørSkolepenger
}

data class InnvilgelseSkolepenger(
    override val begrunnelse: String?,
    override val skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>
) : VedtakSkolepengerDto(
    resultatType = ResultatType.INNVILGE, _type = "InnvilgelseSkolepenger"
)

const val VEDTAK_SKOLEPENGER_OPPHØR_TYPE = "OpphørSkolepenger"

data class OpphørSkolepenger(
    override val begrunnelse: String?,
    override val skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>
) : VedtakSkolepengerDto(resultatType = ResultatType.OPPHØRT, _type = VEDTAK_SKOLEPENGER_OPPHØR_TYPE)

data class SkoleårsperiodeSkolepengerDto(
    val perioder: List<DelårsperiodeSkoleårDto>,
    val utgiftsperioder: List<SkolepengerUtgiftDto>
)

data class DelårsperiodeSkoleårDto(
    val studietype: SkolepengerStudietype,
    val årMånedFra: YearMonth,
    val årMånedTil: YearMonth,
    val studiebelastning: Int,
) {
    fun tilPeriode(): Periode = Periode(this.årMånedFra.atDay(1), this.årMånedTil.atEndOfMonth())

    // Brukes for å ikke være en del av json som blir serialisert
    @delegate:JsonIgnore
    val skoleår: Skoleår by lazy {
        Skoleår(årMånedFra, årMånedTil)
    }
}

data class SkolepengerUtgiftDto(
    val id: UUID,
    val årMånedFra: YearMonth,
    val utgifter: Int,
    val stønad: Int,
)

fun SkoleårsperiodeSkolepengerDto.tilDomene() = SkoleårsperiodeSkolepenger(
    perioder = this.perioder.map { it.tilDomene() },
    utgiftsperioder = this.utgiftsperioder.map {
        SkolepengerUtgift(
            id = it.id,
            utgiftsdato = it.årMånedFra.atDay(1),
            utgifter = it.utgifter,
            stønad = it.stønad
        )
    }
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
        skoleårsperioder = this.skolepenger.skoleårsperioder.map { it.tilDto() }
    )
}

fun Vedtak.mapOpphørSkolepenger(): OpphørSkolepenger {
    feilHvis(this.skolepenger == null) {
        "Mangler felter fra vedtak for vedtak=${this.behandlingId}"
    }
    return OpphørSkolepenger(
        begrunnelse = this.skolepenger.begrunnelse,
        skoleårsperioder = this.skolepenger.skoleårsperioder.map { it.tilDto() }
    )
}

fun SkoleårsperiodeSkolepenger.tilDto() =
    SkoleårsperiodeSkolepengerDto(
        perioder = this.perioder.map { it.tilDto() },
        utgiftsperioder = this.utgiftsperioder.map { it.tilDto() }
    )

fun DelårsperiodeSkoleårSkolepenger.tilDto() = DelårsperiodeSkoleårDto(
    studietype = this.studietype,
    årMånedFra = YearMonth.from(this.datoFra),
    årMånedTil = YearMonth.from(this.datoTil),
    studiebelastning = this.studiebelastning
)

fun SkolepengerUtgift.tilDto() = SkolepengerUtgiftDto(
    id = this.id,
    årMånedFra = YearMonth.from(this.utgiftsdato),
    utgifter = this.utgifter,
    stønad = this.stønad,
)

fun SkoleårsperiodeSkolepengerDto.fraDomeneForSanksjon(): SanksjonertPeriodeDto =
    SanksjonertPeriodeDto(
        årMånedFra = YearMonth.from(this.perioder.single().årMånedFra),
        årMånedTil = YearMonth.from(this.perioder.single().årMånedTil)
    )
