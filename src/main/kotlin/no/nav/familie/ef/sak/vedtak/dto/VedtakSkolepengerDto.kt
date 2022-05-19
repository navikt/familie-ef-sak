package no.nav.familie.ef.sak.vedtak.dto

import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.domain.Skolepengerperiode
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import java.time.YearMonth

data class InnvilgelseSkolepenger(val begrunnelse: String?,
                                  val perioder: List<SkolepengerperiodeDto>)
    : VedtakDto(resultatType = ResultatType.INNVILGE, _type = "InnvilgelseSkolepenger")

data class SkolepengerperiodeDto(
        val studietype: SkolepengerStudietype,
        val årMånedFra: YearMonth,
        val årMånedTil: YearMonth,
        val studiebelastning: Int,
        val utgifter: Int
) {

    fun tilPeriode(): Periode = Periode(this.årMånedFra.atDay(1), this.årMånedTil.atEndOfMonth())
}

fun SkolepengerperiodeDto.tilDomene(): Skolepengerperiode = Skolepengerperiode(
        studietype = this.studietype,
        datoFra = this.årMånedFra.atDay(1),
        datoTil = this.årMånedTil.atEndOfMonth(),
        studiebelastning = this.studiebelastning,
        utgifter = this.utgifter
)

fun Vedtak.mapInnvilgelseSkolepenger(resultatType: ResultatType = ResultatType.INNVILGE): InnvilgelseSkolepenger {
    feilHvis(this.skolepenger == null) {
        "Mangler felter fra vedtak for vedtak=${this.behandlingId}"
    }
    return InnvilgelseSkolepenger(
            begrunnelse = this.skolepenger.begrunnelse,
            perioder = this.skolepenger.perioder.map {
                SkolepengerperiodeDto(studietype = it.studietype,
                                      årMånedFra = YearMonth.from(it.datoFra),
                                      årMånedTil = YearMonth.from(it.datoTil),
                                      studiebelastning = it.studiebelastning,
                                      utgifter = it.utgifter)
            }
    )
}

fun Skolepengerperiode.fraDomeneForSanksjon(): SanksjonertPeriodeDto =
        SanksjonertPeriodeDto(
                årMånedFra = YearMonth.from(datoFra),
                årMånedTil = YearMonth.from(datoTil)
        )