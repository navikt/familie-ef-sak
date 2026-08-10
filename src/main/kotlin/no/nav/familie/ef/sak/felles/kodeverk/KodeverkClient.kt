package no.nav.familie.ef.sak.felles.kodeverk

import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.kodeverk.InntektKodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class KodeverkClient(
    @Qualifier("integrasjonerRestClient") private val restClient: RestClient,
    private val integrasjonerConfig: IntegrasjonerConfig,
) {
    fun hentKodeverkLandkoder(): KodeverkDto =
        restClient
            .get()
            .uri(integrasjonerConfig.kodeverkLandkoderUri)
            .retrieve()
            .body<Ressurs<KodeverkDto>>()!!
            .data!!

    fun hentKodeverkPoststed(): KodeverkDto =
        restClient
            .get()
            .uri(integrasjonerConfig.kodeverkPoststedUri)
            .retrieve()
            .body<Ressurs<KodeverkDto>>()!!
            .data!!

    fun hentKodeverkInntekt(): InntektKodeverkDto =
        restClient
            .get()
            .uri(integrasjonerConfig.kodeverkInntektUri)
            .retrieve()
            .body<Ressurs<InntektKodeverkDto>>()!!
            .data!!
}
