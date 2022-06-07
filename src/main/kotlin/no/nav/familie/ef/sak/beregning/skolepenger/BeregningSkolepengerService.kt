package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.felles.util.skoleår
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.vedtak.dto.SkoleårsperiodeSkolepengerDto
import org.springframework.stereotype.Service
import java.time.Year
import java.util.UUID

private val maksbeløpPerSkoleår = 68_000

@Service
class BeregningSkolepengerService {

    fun beregnYtelse(
        utgiftsperioder: List<SkoleårsperiodeSkolepengerDto>,
        behandlingId: UUID
    ): BeregningSkolepengerResponse {
        return beregnYtelse(utgiftsperioder)
    }

    fun beregnYtelse(
        perioder: List<SkoleårsperiodeSkolepengerDto>,
    ): BeregningSkolepengerResponse {
        validerGyldigePerioder(perioder)
        validerFornuftigeBeløp(perioder)
        validerSkoleår(perioder)

        val perioder = beregnSkoleårsperioder(perioder)
        return BeregningSkolepengerResponse(perioder)
    }

    private fun beregnSkoleårsperioder(
        perioder: List<SkoleårsperiodeSkolepengerDto>
    ): List<BeløpsperiodeSkolepenger> {
        return perioder
            .flatMap { skoleårsperiode -> skoleårsperiode.utgifter }
            .groupBy { it.årMånedFra }
            .toSortedMap()
            .map {
                BeløpsperiodeSkolepenger(
                    årMånedFra = it.key,
                    utgifter = it.value.sumOf { it.utgifter },
                    beløp = it.value.sumOf { it.stønad })
            }
    }

    private fun validerFornuftigeBeløp(skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>) {
        brukerfeilHvis(skoleårsperioder.isEmpty()) {
            "Ingen skoleårsperioder"
        }
        brukerfeilHvis(skoleårsperioder.any { periode -> periode.utgifter.any { it.utgifter < 0 } }) {
            "Utgifter kan ikke være mindre enn 0"
        }
        brukerfeilHvis(skoleårsperioder.any { periode -> periode.utgifter.any { it.stønad > it.utgifter } }) {
            "Stønad kan ikke være høyere enn utgifter"
        }

        skoleårsperioder.forEach { skoleårsperiode ->
            brukerfeilHvis(skoleårsperiode.perioder.any { it.studiebelastning < 1 }) { "Studiebelastning må være over 0" }
            brukerfeilHvis(skoleårsperiode.perioder.any { it.studiebelastning > 100 }) { "Studiebelastning må være under eller lik 100" }
            val skoleår = skoleårsperiode.perioder.first().årMånedFra.skoleår()
            brukerfeilHvis(skoleårsperiode.utgifter.sumOf { it.stønad } > maksbeløpPerSkoleår) {
                "Stønad for skoleåret $skoleår er høyere enn $maksbeløpPerSkoleår"
            }
        }
    }

    private fun validerGyldigePerioder(utgiftsperioder: List<SkoleårsperiodeSkolepengerDto>) {
        brukerfeilHvis(utgiftsperioder.isEmpty()) {
            "Ingen utgiftsperioder"
        }
    }

    private fun validerSkoleår(perioder: List<SkoleårsperiodeSkolepengerDto>) {
        val tidligereSkoleår = mutableSetOf<Year>()
        perioder.forEach { skoleårsperiode ->
            brukerfeilHvisIkke(skoleårsperiode.perioder.all { it.årMånedFra.skoleår() == it.årMånedTil.skoleår() }) {
                "Alle perioder i et skoleår må være det samme skoleåret"
            }
            val skoleår = skoleårsperiode.perioder.first().årMånedFra.skoleår()
            brukerfeilHvisIkke(tidligereSkoleår.add(skoleår)) {
                "Skoleåret $skoleår er definiert flere ganger"
            }
        }
    }
}
