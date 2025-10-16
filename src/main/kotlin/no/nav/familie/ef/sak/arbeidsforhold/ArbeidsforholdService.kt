package no.nav.familie.ef.sak.arbeidsforhold.ekstern

import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.felles.util.isEqualOrBefore
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ArbeidsforholdService(
    private val arbeidsforholdClient: ArbeidsforholdClient,
) {
    fun finnesAvsluttetArbeidsforholdSisteAntallMåneder(
        aktivIdent: String,
        antallMåneder: Long = 6,
    ): Boolean {
        val ansettelsesdato = LocalDate.now().minusMonths(antallMåneder)
        val arbeidsforhold = arbeidsforholdClient.hentArbeidsforhold(aktivIdent)

        return arbeidsforhold.any {
            val startdato = it.ansettelsesperiode?.startdato?.let { dato -> LocalDate.parse(dato) }
            val sluttdato = it.ansettelsesperiode?.sluttdato?.let { dato -> LocalDate.parse(dato) }
            startdato?.isEqualOrBefore(ansettelsesdato) == true &&
                sluttdato?.isEqualOrAfter(ansettelsesdato) == true
        }
    }

    fun finnesNyttArbeidsforholdSisteAntallMåneder(
        aktivIdent: String,
        antallMåneder: Long = 4,
    ): Boolean {
        val ansettelsesdato = LocalDate.now().minusMonths(antallMåneder)
        val arbeidsforhold = arbeidsforholdClient.hentArbeidsforhold(aktivIdent)

        return arbeidsforhold.any {
            val startdato = it.ansettelsesperiode?.startdato?.let { dato -> LocalDate.parse(dato) }
            startdato?.isEqualOrAfter(ansettelsesdato) == true
        }
    }
}
