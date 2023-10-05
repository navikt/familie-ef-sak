package no.nav.familie.ef.sak.opplysninger.personopplysninger.egenansatt

import no.nav.familie.ef.sak.felles.integration.dto.EgenAnsattRequest
import no.nav.familie.ef.sak.felles.integration.dto.EgenAnsattResponse
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class EgenAnsattClient(
    @Value("\${SKJERMEDE_PERSONER_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "egenansatt") {

    private val egenAnsattUri: URI = UriComponentsBuilder.fromUri(uri).pathSegment("skjermet").build().toUri()
    fun egenAnsatt(ident: String): Boolean {
        return postForEntity<Ressurs<EgenAnsattResponse>>(
            egenAnsattUri,
            EgenAnsattRequest(ident),
        ).data!!.erEgenAnsatt
    }
}
