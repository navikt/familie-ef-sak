package no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon

import no.nav.familie.http.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class HistoriskPensjonClient(
    @Value("\${HISTORISK_PENSJON_URL}")
    private val historiskPensjonUri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "pensjon") {

    private fun lagHarPensjonUri() =
        UriComponentsBuilder.fromUri(historiskPensjonUri).pathSegment("api/ensligForsoerger/harPensjonsdata")
            .build().toUri()

    fun harPensjon(aktivIdent: String, alleRelaterteFoedselsnummer: Set<String>): HistoriskPensjonResponse {
        return postForEntity(
            lagHarPensjonUri(),
            EnsligForsoergerRequest(aktivIdent, alleRelaterteFoedselsnummer),
        )
    }
}
