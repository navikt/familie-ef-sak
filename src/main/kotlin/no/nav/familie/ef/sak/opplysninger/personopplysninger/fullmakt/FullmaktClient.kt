package no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.LocalDate

@Service
class FullmaktClient(
    @Value("\${REPR_API_URL}")
    private val fullmaktUrl: String,
    @Qualifier("reprApiRestClient")
    private val restClient: RestClient,
) {
    fun hentFullmakt(ident: String): List<FullmaktResponse> {
        val url = URI.create("$fullmaktUrl/api/internbruker/fullmakt/fullmaktsgiver")
        return restClient
            .post()
            .uri(url)
            .body(FullmaktRequest(ident))
            .retrieve()
            .body<List<FullmaktResponse>>()!!
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
