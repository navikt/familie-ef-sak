package no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker

import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Service
class ArbeidssøkerClient(@Value("\${ARBEIDSSØKER_URL}")
                         private val uri: URI,
                         @Qualifier("azure") restOperations: RestOperations)
    : AbstractRestClient(restOperations, "pdl.personinfo.saksbehandler") {

    fun hentPerioder(personIdent: String, fraOgMed: LocalDate, tilOgMed: LocalDate): ArbeidssøkerResponse {
        val queryUri = UriComponentsBuilder.fromUri(uri).path("arbeidssoker")
                .queryParam("fnr", personIdent)
                .queryParam("fraOgMed", fraOgMed)
                .queryParam("tilOgMed", tilOgMed)
                .build().toUri()
        return getForEntity(queryUri)
    }

}