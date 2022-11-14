package no.nav.familie.ef.sak.felles.kodeverk

import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.http.AbstractPingableRestWebClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.kodeverk.InntektKodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI

@Component
class KodeverkClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Qualifier("azureWebClient") webClient: WebClient,
    private val integrasjonerConfig: IntegrasjonerConfig,
    featureToggleService: FeatureToggleService
) :
    AbstractPingableRestWebClient(restOperations, webClient, "kodeverk", featureToggleService) {

    override val pingUri: URI = integrasjonerConfig.pingUri
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentKodeverkLandkoder(): KodeverkDto {
        return getForEntity<Ressurs<KodeverkDto>>(integrasjonerConfig.kodeverkLandkoderUri).data!!
    }

    fun hentKodeverkPoststed(): KodeverkDto {
        return getForEntity<Ressurs<KodeverkDto>>(integrasjonerConfig.kodeverkPoststedUri).data!!
    }

    fun hentKodeverkInntekt(): InntektKodeverkDto {
        return getForEntity<Ressurs<InntektKodeverkDto>>(integrasjonerConfig.kodeverkInntektUri).data!!
    }
}
