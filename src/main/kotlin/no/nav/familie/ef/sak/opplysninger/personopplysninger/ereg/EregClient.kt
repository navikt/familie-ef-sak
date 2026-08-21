package no.nav.familie.ef.sak.opplysninger.personopplysninger.ereg

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Kaller ereg-services direkte istedenfor via familie-ef-proxy.
 * Krever ingen Azure AD-token, kun nettverkstilgang (webproxy/access policy).
 */
@Component
class EregClient(
    @Value("\${EREG_URL}")
    private val eregUri: URI,
    @Qualifier("utenAuthRestClient")
    private val restClient: RestClient,
) {
    fun hentOrganisasjoner(organisasjonsnumre: List<String>): List<OrganisasjonDto> = organisasjonsnumre.map(::hentOrganisasjon)

    private fun hentOrganisasjon(organisasjonsnummer: String): OrganisasjonDto {
        val uri =
            UriComponentsBuilder
                .fromUri(eregUri)
                .pathSegment("v1", "organisasjon", organisasjonsnummer)
                .build()
                .toUri()

        return restClient
            .get()
            .uri(uri)
            .retrieve()
            .body<OrganisasjonDto>()!!
    }
}
