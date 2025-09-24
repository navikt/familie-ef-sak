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
        return arbeidsforholdResponse.data ?: emptyList()
    }

    fun finnesAvsluttetArbeidsforholdSisteAntallMåneder(
        aktivIdent: String,
        antallMåneder: Long = 6,
    ): Boolean {
        val ansettelsesdato = LocalDate.now().minusMonths(antallMåneder)
        val arbeidsforhold = arbeidsforholdClient.hentArbeidsforhold(aktivIdent).data

        return arbeidsforhold?.any {
            it.ansettelsesperiode
                ?.periode
                ?.fom
                ?.isEqualOrBefore(ansettelsesdato) == true &&
                it.ansettelsesperiode
                    ?.periode
                    ?.tom
                    ?.isEqualOrAfter(ansettelsesdato) == true
        } == true
    }
}
