package no.nav.familie.ef.sak.opplysninger.personopplysninger.ereg

import no.nav.familie.http.client.AbstractPingableRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class EregClient(
    @Value("\${FAMILIE_EF_PROXY_URL}")
    private val familieEfProxyUri: String,
    @Qualifier("azure")
    private val restOperations: RestOperations
) : AbstractPingableRestClient(restOperations, "familie.ef.iverksett") {

    fun hentOrganisasjoner(organisasjonsnumre: List<String>) : List<OrganisasjonDto> {
        return getForEntity(URI.create("$familieEfProxyUri/api/ereg?organisasjonsnumre=$organisasjonsnumre"))
    }

    override val pingUri = URI.create(familieEfProxyUri)

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }

}