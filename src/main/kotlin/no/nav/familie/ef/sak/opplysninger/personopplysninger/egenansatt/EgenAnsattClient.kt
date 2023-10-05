package no.nav.familie.ef.sak.opplysninger.personopplysninger.egenansatt

import no.nav.familie.ef.sak.felles.integration.dto.EgenAnsattRequest
import no.nav.familie.ef.sak.felles.integration.dto.EgenAnsattResponse
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class EgenAnsattClient(
    @Value("\${SKJERMING_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "egenansatt") {

    fun egenAnsatt(ident: String): Boolean {
        return postForEntity<Ressurs<EgenAnsattResponse>>(
            uri,
            EgenAnsattRequest(ident),
        ).data!!.erEgenAnsatt
    }
}
