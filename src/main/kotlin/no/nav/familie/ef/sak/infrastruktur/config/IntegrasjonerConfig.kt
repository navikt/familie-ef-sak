package no.nav.familie.ef.sak.infrastruktur.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Configuration
class IntegrasjonerConfig(
    @Value("\${FAMILIE_INTEGRASJONER_URL}") private val integrasjonUri: URI,
) {
    val pingUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_PING).build().toUri()

    val tilgangRelasjonerUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_TILGANG_RELASJONER).build().toUri()
    val tilgangPersonUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_TILGANG_PERSON).build().toUri()

    val adressebeskyttelse: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_ADRESSEBESKYTTELSE).build().toUri()

    val kodeverkLandkoderUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_KODEVERK_LANDKODER).build().toUri()

    val kodeverkPoststedUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_KODEVERK_POSTSTED).build().toUri()

    val kodeverkInntektUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_KODEVERK_INNTEKT).build().toUri()

    val arbeidsfordelingUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_ARBEIDSFORDELING).build().toUri()

    val arbeidsfordelingMedRelasjonerUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_BEHANDLENDE_ENHET_MED_RELASJONER).build().toUri()

    val arbeidsforholdUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_ARBEIDSFORHOLD).build().toUri()

    val oppgaveUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_OPPGAVE).build().toUri()

    val saksbehandlerUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_SAKSBEHANDLER).build().toUri()

    val journalPostUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_JOURNALPOST).build().toUri()

    val dokarkivUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_DOKARKIV).build().toUri()

    val medlemskapUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_MEDLEMSKAP).build().toUri()

    val navKontorUri: URI = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_NAV_KONTOR).build().toUri()

    val arbeidsfordelingOppfølgingUri: URI =
        UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(
            PATH_ARBEIDSFORDELING_OPPFØLGING,
        ).build().toUri()

    companion object {
        private const val PATH_PING = "api/ping"
        private const val PATH_KODEVERK_LANDKODER = "api/kodeverk/landkoder"
        private const val PATH_KODEVERK_POSTSTED = "api/kodeverk/poststed"
        private const val PATH_KODEVERK_INNTEKT = "api/kodeverk/inntekt"
        private const val PATH_TILGANG_RELASJONER = "api/tilgang/person-med-relasjoner"
        private const val PATH_TILGANG_PERSON = "api/tilgang/v2/personer"
        private const val PATH_ADRESSEBESKYTTELSE = "api/personopplysning/strengeste-adressebeskyttelse-for-person-med-relasjoner"
        private const val PATH_ARBEIDSFORDELING = "api/arbeidsfordeling/enhet/ENF"
        private const val PATH_BEHANDLENDE_ENHET_MED_RELASJONER = "api/arbeidsfordeling/enhet/ENF/med-relasjoner"
        private const val PATH_ARBEIDSFORHOLD = "api/aareg/arbeidsforhold"
        private const val PATH_OPPGAVE = "api/oppgave"
        private const val PATH_JOURNALPOST = "api/journalpost"
        private const val PATH_DOKARKIV = "api/arkiv"
        private const val PATH_MEDLEMSKAP = "api/medlemskap/v3"
        private const val PATH_NAV_KONTOR = "api/arbeidsfordeling/nav-kontor/ENF"
        private const val PATH_ARBEIDSFORDELING_OPPFØLGING = "api/arbeidsfordeling/oppfolging/ENF"
        private const val PATH_SAKSBEHANDLER = "api/saksbehandler"
    }
}
