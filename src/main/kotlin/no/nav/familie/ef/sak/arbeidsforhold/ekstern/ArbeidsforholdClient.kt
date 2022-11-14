package no.nav.familie.ef.sak.arbeidsforhold.ekstern

import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.http.AbstractRestWebClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.arbeidsforhold.Arbeidsforhold
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Component
class ArbeidsforholdClient(
    @Value("\${FAMILIE_INTEGRASJONER_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
    @Qualifier("azureWebClient") webClient: WebClient,
    featureToggleService: FeatureToggleService
) : AbstractRestWebClient(restOperations, webClient, "arbeidsforhold", featureToggleService) {

    private fun lagArbeidsforholdUri() =
        UriComponentsBuilder.fromUri(uri).pathSegment("api/aareg/arbeidsforhold").build().toUri()

    fun hentArbeidsforhold(personIdent: String, ansettelsesperiodeFom: LocalDate): Ressurs<List<Arbeidsforhold>> {
        return postForEntity(lagArbeidsforholdUri(), ArbeidsforholdRequest(personIdent, ansettelsesperiodeFom))
    }
}

class ArbeidsforholdRequest(
    val personIdent: String,
    val ansettelsesperiodeFom: LocalDate
)
