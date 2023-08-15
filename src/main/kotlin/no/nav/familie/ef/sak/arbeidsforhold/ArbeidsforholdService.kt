package no.nav.familie.ef.sak.arbeidsforhold.ekstern

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidsforhold
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class ArbeidsforholdService(
    private val fagsakService: FagsakService,
    private val arbeidsforholdClient: ArbeidsforholdClient,
) {

    fun hentArbeidsforhold(fagsakId: UUID, ansettelsesperiodeFom: LocalDate): List<Arbeidsforhold> {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val arbeidsforholdResponse = arbeidsforholdClient.hentArbeidsforhold(aktivIdent, ansettelsesperiodeFom)
        return arbeidsforholdResponse.data ?: emptyList()
    }

    fun finnesAvsluttetArbeidsforholdSisteGittAntallMåneder(aktivIdent: String, antallMåneder: Long = 6): Boolean {
        val arbeidsforhold = arbeidsforholdClient.hentArbeidsforhold(aktivIdent, LocalDate.now().minusMonths(6)).data

        return arbeidsforhold?.any {
            it.ansettelsesperiode?.periode?.fom?.isBefore(LocalDate.now().minusMonths(antallMåneder)) == true &&
                it.ansettelsesperiode?.periode?.tom?.isAfter(LocalDate.now().minusMonths(antallMåneder)) == true
        } == true
    }
}
