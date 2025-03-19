package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

// TODO: Denne skal fjernes, kun for testing gjennom Swagger.
@Component
class PersonHendelseClient(
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "familie.ef-personhendelse") {
    private val uri: URI = URI("https://familie-ef-personhendelse.intern.dev.nav.no")

    // TODO: Denne skal fjernes, kun for testing gjennom Swagger.
    fun opprettBehandleAutomatiskInntektsendringTask(personIdent: String): Boolean {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/inntekt/manuellOpprettelseAvBehandleAutomatiskInntektsendringTask")
        val response = postForEntity<Ressurs<Boolean>>(uriComponentsBuilder.build().toUri(), personIdent)
        return response.data ?: throw Exception("Feil ved kall mot ef-personhendelse med personIdent for oppretting av behandle automatisk inntektsendring task.")
    }

    // TODO: Denne skal fjernes, kun for testing gjennom Swagger.
    fun opprettBehandleAutomatiskInntektsendringTasker(): Boolean {
        val uriComponentsBuilder =
            UriComponentsBuilder
                .fromUri(uri)
                .pathSegment("api/inntekt/manuellOpprettelseAvBehandleAutomatiskInntektsendringTasker")
        val response = postForEntity<Ressurs<Boolean>>(uriComponentsBuilder.build().toUri(), Any())
        return response.data ?: throw Exception("Feil ved kall mot ef-personhendelse for generering av behandle automatisk inntektsendring tasker.")
    }
}
