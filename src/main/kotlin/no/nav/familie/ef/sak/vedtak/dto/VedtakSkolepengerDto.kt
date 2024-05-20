package no.nav.familie.ef.sak.vedtak.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ef.sak.felles.util.Skoleår
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.domain.DelårsperiodeSkoleårSkolepenger
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerUtgift
import no.nav.familie.ef.sak.vedtak.domain.SkoleårsperiodeSkolepenger
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.kontrakter.felles.Månedsperiode
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
    _type: String,
) : VedtakDto(resultatType, _type) {
    abstract val begrunnelse: String?
    abstract val skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>

    fun erOpphør() = this is OpphørSkolepenger
}

data class InnvilgelseSkolepenger(
    override val begrunnelse: String?,
    override val skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>,
) : VedtakSkolepengerDto(
        resultatType = ResultatType.INNVILGE,
        _type = "InnvilgelseSkolepenger",
    )

const val VEDTAK_SKOLEPENGER_OPPHØR_TYPE = "OpphørSkolepenger"

data class OpphørSkolepenger(
    override val begrunnelse: String?,
    override val skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>,
) : VedtakSkolepengerDto(resultatType = ResultatType.OPPHØRT, _type = VEDTAK_SKOLEPENGER_OPPHØR_TYPE)

data class SkoleårsperiodeSkolepengerDto(
    val perioder: List<DelårsperiodeSkoleårDto>,
    val utgiftsperioder: List<SkolepengerUtgiftDto>,
)

data class DelårsperiodeSkoleårDto(
    val studietype: SkolepengerStudietype,
    @Deprecated("Bruke periode", ReplaceWith("periode.fom")) val årMånedFra: YearMonth? = null,
    @Deprecated("Bruke periode", ReplaceWith("periode.tom")) val årMånedTil: YearMonth? = null,
    @JsonIgnore
    val periode: Månedsperiode =
        Månedsperiode(
            årMånedFra ?: error("periode eller årMånedFra må ha verdi"),
            årMånedTil ?: error("periode eller årMånedTil må ha verdi"),
        ),
    val studiebelastning: Int,
) {
    // Brukes for å ikke være en del av json som blir serialisert
    @delegate:JsonIgnore
    val skoleår: Skoleår by lazy {
        Skoleår(periode)
    }
}

data class SkolepengerUtgiftDto(
    val id: UUID,
    val årMånedFra: YearMonth,
    val stønad: Int,
)

fun SkoleårsperiodeSkolepengerDto.tilDomene() =
    SkoleårsperiodeSkolepenger(
        perioder = this.perioder.map { it.tilDomene() }.sortedBy { it.periode },
        utgiftsperioder =
            this.utgiftsperioder.map {
                SkolepengerUtgift(
                    id = it.id,
                    utgiftsdato = it.årMånedFra.atDay(1),
                    stønad = it.stønad,
                )
            }.sortedBy { it.utgiftsdato },
    )

fun DelårsperiodeSkoleårDto.tilDomene() =
    DelårsperiodeSkoleårSkolepenger(
        studietype = this.studietype,
        periode = this.periode,
        studiebelastning = this.studiebelastning,
    )

fun Vedtak.mapInnvilgelseSkolepenger(): InnvilgelseSkolepenger {
    feilHvis(this.skolepenger == null) {
        "Mangler felter fra vedtak for vedtak=${this.behandlingId}"
    }
    return InnvilgelseSkolepenger(
        begrunnelse = this.skolepenger.begrunnelse,
        skoleårsperioder = this.skolepenger.skoleårsperioder.map { it.tilDto() },
    )
}

fun Vedtak.mapOpphørSkolepenger(): OpphørSkolepenger {
    feilHvis(this.skolepenger == null) {
        "Mangler felter fra vedtak for vedtak=${this.behandlingId}"
    }
    return OpphørSkolepenger(
        begrunnelse = this.skolepenger.begrunnelse,
        skoleårsperioder = this.skolepenger.skoleårsperioder.map { it.tilDto() },
    )
}

fun SkoleårsperiodeSkolepenger.tilDto() =
    SkoleårsperiodeSkolepengerDto(
        perioder = this.perioder.map { it.tilDto() },
        utgiftsperioder = this.utgiftsperioder.map { it.tilDto() },
    )

fun DelårsperiodeSkoleårSkolepenger.tilDto() =
    DelårsperiodeSkoleårDto(
        studietype = this.studietype,
        årMånedFra = YearMonth.from(this.datoFra),
        årMånedTil = YearMonth.from(this.datoTil),
        periode = Månedsperiode(this.datoFra, this.datoTil),
        studiebelastning = this.studiebelastning,
    )

fun SkolepengerUtgift.tilDto() =
    SkolepengerUtgiftDto(
        id = this.id,
        årMånedFra = YearMonth.from(this.utgiftsdato),
        stønad = this.stønad,
    )
