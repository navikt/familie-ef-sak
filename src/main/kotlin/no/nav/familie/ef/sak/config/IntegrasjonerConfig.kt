package no.nav.familie.ef.sak.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
class IntegrasjonerConfig(@Value("\${FAMILIE_INTEGRASJONER_URL}") private val integrasjonUri: URI) {

    val pingUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).path(PATH_PING).build().toUri()

    val tilgangUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_TILGANGER).build().toUri()

    val personopplysningerUri: URI =
            UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_PERSONOPPLYSNING).build().toUri()

    val personhistorikkUri: URI =
            UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_PERSONHISTORIKK).build().toUri()

    val personhistorikkUriBuilder: UriComponentsBuilder =
            UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_PERSONHISTORIKK)

    val kodeverkLandkoderUri: URI =
            UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_KODEVERK_LANDKODER).build().toUri()

    val kodeverkPoststedUri: URI =
            UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_KODEVERK_POSTSTED).build().toUri()

    val egenAnsattUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_EGEN_ANSATT).build().toUri()

    companion object {
        private const val PATH_PING = "/internal/status/isAlive"
        private const val PATH_KODEVERK_LANDKODER = "api/kodeverk/landkoder"
        private const val PATH_KODEVERK_POSTSTED = "api/kodeverk/poststed"
        private const val PATH_TILGANGER = "api/tilgang/personer"
        private const val PATH_PERSONOPPLYSNING = "api/personopplysning/v1/info"
        private const val PATH_PERSONHISTORIKK = "api/personopplysning/v1/historikk"
        private const val PATH_EGEN_ANSATT = "api/egenansatt"
    }
}
