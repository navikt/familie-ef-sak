package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseSkolepenger
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeSkolepengerDto
import org.springframework.stereotype.Service

@Service
class BeregningSkolepengerService {

    fun beregnYtelse(innvilgelse: InnvilgelseSkolepenger): List<BeløpsperiodeSkolepengerDto> {
        return beregnYtelse(innvilgelse.perioder)
    }

    fun beregnYtelse(utgiftsperioder: List<UtgiftsperiodeSkolepengerDto>): List<BeløpsperiodeSkolepengerDto> {
        validerGyldigePerioder(utgiftsperioder)
        validerFornuftigeBeløp(utgiftsperioder)

        return utgiftsperioder.map {
            BeløpsperiodeSkolepengerDto(
                it.tilPeriode(),
                beløp = 100, // TODO håndtere senere
                BeregningsgrunnlagSkolepengerDto(it.studietype, it.utgifter, it.studiebelastning)
            )
        }
    }

    private fun validerFornuftigeBeløp(utgiftsperioder: List<UtgiftsperiodeSkolepengerDto>) {

        brukerfeilHvis(utgiftsperioder.any { it.utgifter < 0 }) { "Utgifter kan ikke være mindre enn 0" }

        brukerfeilHvis(utgiftsperioder.any { it.studiebelastning < 1 }) { "Studiebelastning må være over 0" }
        brukerfeilHvis(utgiftsperioder.any { it.studiebelastning > 100 }) { "Studiebelastning må være under eller lik 100" }
    }

    private fun validerGyldigePerioder(utgiftsperioder: List<UtgiftsperiodeSkolepengerDto>) {
        brukerfeilHvis(utgiftsperioder.isEmpty()) {
            "Ingen utgiftsperioder"
        }
    }
}
