package no.nav.familie.ef.sak.felles.kodeverk

import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.kodeverk.InntektKodeverkDto
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

@Component
class KodeverkClient(
    @Qualifier("azure") restOperations: RestOperations,
    private val integrasjonerConfig: IntegrasjonerConfig,
) : AbstractPingableRestClient(restOperations, "kodeverk") {
    override val pingUri: URI = integrasjonerConfig.pingUri
    private val logger = Logg.getLogger(this::class)

    fun hentKodeverkLandkoder(): KodeverkDto = getForEntity<Ressurs<KodeverkDto>>(integrasjonerConfig.kodeverkLandkoderUri).data!!

    fun hentKodeverkPoststed(): KodeverkDto = getForEntity<Ressurs<KodeverkDto>>(integrasjonerConfig.kodeverkPoststedUri).data!!

    fun hentKodeverkInntekt(): InntektKodeverkDto = getForEntity<Ressurs<InntektKodeverkDto>>(integrasjonerConfig.kodeverkInntektUri).data!!
}
