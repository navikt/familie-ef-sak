package no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.FullmaktDto
import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI

@Service
class FullmaktClient(
    @Value("\${PDL_FULLMAKT_URL}")
    private val fullmaktUrl: String,
    @Qualifier("azure")
    private val restOperations: RestOperations,
) : AbstractPingableRestClient(restOperations, "repr.fullmakt") {
    override val pingUri: URI = URI.create(fullmaktUrl)

    fun hentFullmakt(ident: String): FullmaktDto {
        val url = URI.create("$fullmaktUrl/api/internbruker/fullmektig")
        return postForEntity(url, FullmaktRequest(ident))
    }
}

data class FullmaktRequest(
    val ident: String,
)