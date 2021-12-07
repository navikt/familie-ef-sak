package no.nav.familie.ef.sak.organisasjon

import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class OrganisasjonClient(@Qualifier("azure") restOperations: RestOperations,
                         integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "organisasjon") {

    override val pingUri: URI = integrasjonerConfig.pingUri
    private val organisasjonURI: URI = integrasjonerConfig.organisasjonURI

    fun hentOrganisasjon(orgnr: String): Organisasjon {
        return getForEntity<Ressurs<Organisasjon>>(URI.create("${organisasjonURI}/${orgnr}")).getDataOrThrow()
    }

}
