package no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI
import java.time.LocalDate

// Feilkode fra tilgangsmaskinen når saksbehandler er geografisk avvist tilgang til å slå opp fullmakt
private const val SAKSBEHANDLER_AVVIST_TILGANGSMASKINEN_GEOGRAFISK = "SAKSBEHANDLER_AVVIST_TILGANGSMASKINEN_GEOGRAFISK"

@Service
class FullmaktClient(
    @Value("\${REPR_API_URL}")
    private val fullmaktUrl: String,
    @Qualifier("reprApiRestClient")
    private val restClient: RestClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * @return liste med fullmakter, eller `null` dersom vi ikke fikk hentet fullmakt fordi saksbehandler
     * mangler geografisk tilgang i tilgangsmaskinen. `null` skal tolkes som "ukjent" og ikke "ingen fullmakt".
     */
    fun hentFullmakt(ident: String): List<FullmaktResponse>? {
        val url = URI.create("$fullmaktUrl/api/internbruker/fullmakt/fullmaktsgiver")
        return try {
            restClient
                .post()
                .uri(url)
                .body(FullmaktRequest(ident))
                .retrieve()
                .body<List<FullmaktResponse>>()!!
        } catch (e: HttpClientErrorException.Forbidden) {
            if (e.responseBodyAsString.contains(SAKSBEHANDLER_AVVIST_TILGANGSMASKINEN_GEOGRAFISK)) {
                logger.warn("Saksbehandler mangler geografisk tilgang i tilgangsmaskinen til å hente fullmakt. Viser fullmakt som ukjent.")
                null
            } else {
                throw e
            }
        }
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
