package no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon

import no.nav.familie.http.client.AbstractRestClient
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun lagHarPensjonUri() =
        UriComponentsBuilder.fromUri(historiskPensjonUri).pathSegment("api/ensligForsoerger/harPensjonsdata")
            .build().toUri()

    val manglendeRespons = HistoriskPensjonResponse(false, "")

    fun hentHistoriskPensjonStatusForIdent(
        aktivIdent: String,
        alleRelaterteFoedselsnummer: Set<String>,
    ): HistoriskPensjonDto {
        repeat(2) {
            try {
                return postForEntity<HistoriskPensjonResponse>(
                    lagHarPensjonUri(),
                    EnsligForsoergerRequest(aktivIdent, alleRelaterteFoedselsnummer),
                ).tilDto()
            } catch (e: Exception) {
                logger.error("Kunne ikke kalle historisk pensjon for uthenting")
                secureLogger.error("Kunne ikke kalle historisk pensjon for uthenting", e)
            }
        }
        return HistoriskPensjonDto(HistoriskPensjonStatus.UKJENT, null)
    }
}
