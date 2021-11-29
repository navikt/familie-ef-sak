package no.nav.familie.ef.sak.arbeidsforhold.ekstern

import no.nav.familie.ef.sak.arbeidsforhold.ArbeidsforholdDto
import no.nav.familie.ef.sak.arbeidsforhold.tilDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class ArbeidsforholdService(
    private val fagsakService: FagsakService,
    private val arbeidsforholdClient: ArbeidsforholdClient
) {

    fun hentArbeidsforhold(fagsakId: UUID, ansettelsesperiodeFom: LocalDate): List<ArbeidsforholdDto> {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val arbeidsforholdResponse = arbeidsforholdClient.hentArbeidsforhold(aktivIdent, ansettelsesperiodeFom)
        return arbeidsforholdResponse.data?.tilDto() ?: emptyList()

    }
}