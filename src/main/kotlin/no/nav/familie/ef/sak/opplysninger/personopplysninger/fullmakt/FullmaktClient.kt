package no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt

import no.nav.familie.kontrakter.felles.jsonMapper
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
     * Fullmakt kan ikke hentes ut dersom innlogget bruker mangler tilgang i tilgangsmaskinen (403 Forbidden).
     * Vi vet ikke på forhånd om, eller hvorfor, dette skjer - det er svaret fra tilgangsmaskinen som avgjør dette.
     * Kallet gir derfor aldri feil videre til kalleren ved 403, men returnerer i stedet [FullmaktOppslagResultat]
     * med `fullmakter = null` og en beskrivelse av årsaken, slik at kalleren selv kan avgjøre hvordan dette skal
     * håndteres/vises videre (se [FullmaktService.hentFullmakt]).
     */
    fun hentFullmakt(ident: String): FullmaktOppslagResultat {
        val url = URI.create("$fullmaktUrl/api/internbruker/fullmakt/fullmaktsgiver")
        return try {
            val fullmakter =
                restClient
                    .post()
                    .uri(url)
                    .body(FullmaktRequest(ident))
                    .retrieve()
                    .body<List<FullmaktResponse>>()!!
            FullmaktOppslagResultat(fullmakter = fullmakter)
        } catch (e: HttpClientErrorException.Forbidden) {
            val årsak = utledÅrsak(e)
            logger.warn("Mangler tilgang til å hente fullmakt i tilgangsmaskinen. Årsak: $årsak. Viser fullmakt som ukjent.")
            FullmaktOppslagResultat(fullmakter = null, ikkeTilgangÅrsak = årsak)
        }
    }

    private fun utledÅrsak(e: HttpClientErrorException.Forbidden): String {
        val errorCode = hentErrorCode(e.responseBodyAsString)
        return when (errorCode) {
            SAKSBEHANDLER_AVVIST_TILGANGSMASKINEN_GEOGRAFISK -> "mangler geografisk tilgang i tilgangsmaskinen"
            null -> e.responseBodyAsString.ifBlank { e.message ?: "ukjent årsak" }
            else -> "tilgangsmaskinen avviste kallet med feilkode $errorCode"
        }
    }

    private fun hentErrorCode(responseBody: String): String? =
        try {
            jsonMapper
                .readTree(responseBody)
                .path("errorCode")
                .asText()
                .takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
}

data class FullmaktOppslagResultat(
    val fullmakter: List<FullmaktResponse>?,
    val ikkeTilgangÅrsak: String? = null,
)

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
