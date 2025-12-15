package no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt

import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Service
class FullmaktClient(
    @Value("\${REPR_API_URL}")
    private val fullmaktUrl: String,
    @Qualifier("azure")
    private val restOperations: RestOperations,
) : AbstractPingableRestClient(restOperations, "repr.fullmakt") {
    override val pingUri: URI = URI.create(fullmaktUrl)

    fun hentFullmakt(ident: String): List<FullmaktResponse> {
        val url = URI.create("$fullmaktUrl/api/internbruker/fullmakt/fullmaktsgiver")
        val fullmaktResponse = postForEntity<List<FullmaktResponse>>(url, FullmaktRequest(ident))
        return fullmaktResponse
    }
}

data class FullmaktRequest(
    val ident: String,
)

data class FullmaktResponse(
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate?,
    val fullmektig: String,
    val fullmektigsNavn: String?,
    val omraade: List<Område>,
)

data class Område(
    val tema: String,
    val handling: List<Handling>,
)

enum class Handling { LES, KOMMUNISER, SKRIV }
