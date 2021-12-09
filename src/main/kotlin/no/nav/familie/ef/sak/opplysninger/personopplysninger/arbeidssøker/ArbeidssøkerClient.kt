package no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Service
class ArbeidssøkerClient(@Value("\${FAMILIE_EF_PROXY_URL}")
                         private val uri: URI,
                         @Qualifier("azure") restOperations: RestOperations)
    : AbstractRestClient(restOperations, "pdl.personinfo.saksbehandler") {

    fun hentPerioder(personIdent: String, fraOgMed: LocalDate, tilOgMed: LocalDate? = null): ArbeidssøkerResponse {
        val uriBuilder = UriComponentsBuilder.fromUri(uri).pathSegment("api/arbeidssoker/perioder")
                .queryParam("fraOgMed", fraOgMed)
        tilOgMed?.let { uriBuilder.queryParam("tilOgMed", tilOgMed) }

        return postForEntity(uriBuilder.build().toUri(), PersonIdent(personIdent))
    }

}