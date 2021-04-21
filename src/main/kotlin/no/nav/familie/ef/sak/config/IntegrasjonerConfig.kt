package no.nav.familie.ef.sak.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
class IntegrasjonerConfig(@Value("\${FAMILIE_INTEGRASJONER_URL}") private val integrasjonUri: URI) {

    val pingUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_PING).build().toUri()

    val tilgangRelasjonerUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_TILGANG_RELASJONER).build().toUri()

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

    val arbeidsfordelingUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_ARBEIDSFORDELING).build().toUri()
    val oppgaveUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_OPPGAVE).build().toUri()

    val journalPostUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_JOURNALPOST).build().toUri()
    val dokumentIdUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_JOURNALPOST).build().toUri()

    val dokarkivUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_DOKARKIV).build().toUri()

    val medlemskapUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_MEDLEMSKAP).build().toUri()

    val infotrygdVedtaksperioder =
            UriComponentsBuilder.fromUri(integrasjonUri).path("api/infotrygd/vedtak-perioder").build().toUri()

    val distribuerDokumentUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_DISTRIBUER_DOKUMENT).build().toUri()

    val navKontorUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_NAV_KONTOR).build().toUri()

    companion object {

        private const val PATH_PING = "api/ping"
        private const val PATH_KODEVERK_LANDKODER = "api/kodeverk/landkoder"
        private const val PATH_KODEVERK_POSTSTED = "api/kodeverk/poststed"
        private const val PATH_TILGANG_RELASJONER = "api/tilgang/person-med-relasjoner"
        private const val PATH_PERSONOPPLYSNING = "api/personopplysning/v2/info"
        private const val PATH_PERSONHISTORIKK = "api/personopplysning/v2/historikk"
        private const val PATH_EGEN_ANSATT = "api/egenansatt"
        private const val PATH_ARBEIDSFORDELING = "api/arbeidsfordeling/enhet/ENF"
        private const val PATH_OPPGAVE = "api/oppgave"
        private const val PATH_JOURNALPOST = "api/journalpost"
        private const val PATH_DOKARKIV = "api/arkiv"
        private const val PATH_MEDLEMSKAP = "api/medlemskap/v3"
        private const val PATH_DISTRIBUER_DOKUMENT = "api/dist/v1"
        private const val PATH_NAV_KONTOR = "api/arbeidsfordeling/nav-kontor/ENF"

    }
}
