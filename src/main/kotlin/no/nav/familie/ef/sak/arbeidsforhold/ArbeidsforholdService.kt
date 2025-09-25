package no.nav.familie.ef.sak.arbeidsforhold.ekstern

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.felles.util.isEqualOrBefore
import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidsforhold
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class ArbeidsforholdService(
    private val fagsakService: FagsakService,
    private val arbeidsforholdClient: ArbeidsforholdClient,
) {
    fun hentArbeidsforhold(
        fagsakId: UUID,
    ): List<Arbeidsforhold> {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val arbeidsforholdResponse = arbeidsforholdClient.hentArbeidsforhold(aktivIdent)
        return arbeidsforholdResponse
    }

    fun finnesAvsluttetArbeidsforholdSisteAntallMåneder(
        aktivIdent: String,
        antallMåneder: Long = 6,
    ): Boolean {
        val ansettelsesdato = LocalDate.now().minusMonths(antallMåneder)
        val arbeidsforhold = arbeidsforholdClient.hentArbeidsforhold(aktivIdent)

        return arbeidsforhold?.any {
            val startdato = it.ansettelsesperiode?.startdato?.let { dato -> LocalDate.parse(dato) }
            val sluttdato = it.ansettelsesperiode?.sluttdato?.let { dato -> LocalDate.parse(dato) }
            startdato?.isEqualOrBefore(ansettelsesdato) == true &&
                sluttdato?.isEqualOrAfter(ansettelsesdato) == true
        } == true
    }
}
