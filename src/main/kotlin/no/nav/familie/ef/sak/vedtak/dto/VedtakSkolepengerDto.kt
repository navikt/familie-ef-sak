package no.nav.familie.ef.sak.vedtak.dto

import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import java.time.YearMonth

data class InnvilgelseSkolepenger(val perioder: List<Skolepengerperiode>,
                                  val begrunnelse: String?)
    : VedtakDto(resultatType = ResultatType.INNVILGE, _type = "InnvilgelseSkolepenger")

data class Skolepengerperiode(
        val årMånedFra: YearMonth,
        val årMånedTil: YearMonth,
        val beløp: Int
) {

    fun tilPeriode(): Periode = Periode(this.årMånedFra.atDay(1), this.årMånedTil.atEndOfMonth())
}

fun Vedtak.mapInnvilgelseSkolepenger(resultatType: ResultatType = ResultatType.INNVILGE): InnvilgelseSkolepenger {
    feilHvis(this.skolepenger == null) {
        "Mangler felter fra vedtak for vedtak=${this.behandlingId}"
    }
    return InnvilgelseSkolepenger(
            begrunnelse = this.skolepenger.begrunnelse,
            perioder = this.skolepenger.perioder.map {
                Skolepengerperiode(årMånedFra = YearMonth.from(it.datoFra),
                                   årMånedTil = YearMonth.from(it.datoTil),
                                   beløp = it.utgifter.toInt())
            },
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