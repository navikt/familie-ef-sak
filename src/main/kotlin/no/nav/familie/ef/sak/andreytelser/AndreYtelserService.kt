package no.nav.familie.ef.sak.andreytelser

import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class AndreYtelserService(
    private val fagsakPersonService: FagsakPersonService,
    private val arbeidsavklaringspengerClient: ArbeidsavklaringspengerClient,

    ) {
    fun hentAndreYtelser(fagsakPersonId: UUID): AndreYtelserDto {
        val personIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        val request = ArbeidsavklaringspengerRequest(
            fraOgMedDato = LocalDate.now().minusYears(1),
            tilOgMedDato = LocalDate.now().plusYears(1),
            personidentifikator = personIdent,
        )

        val arbeidsavklaringspengerResponse = arbeidsavklaringspengerClient.hentPerioder(request)

        return AndreYtelserDto(arbeidsavklaringspenger = arbeidsavklaringspengerResponse)
    }
}
