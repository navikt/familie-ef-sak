package no.nav.familie.ef.sak.opplysninger.personopplysninger.medl

import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Component
class MedlClient(
    @Value("\${MEDL_URL}") private val uri: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "familie.medl") {
    val soekUri =
        UriComponentsBuilder
            .fromUri(uri)
            .pathSegment("rest/v1/periode/soek")
            .build()
            .toUri()

    fun hentMedlemskapsUnntak(
        personident: String,
        type: String? = null,
        statuser: List<String>? = null,
        ekskluderKilder: List<String>? = null,
        fraOgMed: LocalDate? = null,
        tilOgMed: LocalDate? = null,
    ): List<Medlemskapsunntak> {
        val requestBody =
            PeriodeSoekRequest(
                personident = personident,
                type = type,
                statuser = statuser,
                ekskluderKilder = ekskluderKilder,
                fraOgMed = fraOgMed,
                tilOgMed = tilOgMed,
            )

        return postForEntity<List<Medlemskapsunntak>>(soekUri, requestBody)
    }
}

data class PeriodeSoekRequest(
    val personident: String,
    val type: String? = null,
    val statuser: List<String>? = null,
    val ekskluderKilder: List<String>? = null,
    val fraOgMed: LocalDate? = null,
    val tilOgMed: LocalDate? = null,
)

data class Medlemskapsunntak(
    val dekning: String? = null,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val grunnlag: String,
    val ident: String,
    val medlem: Boolean,
    val status: String,
    val statusaarsak: String? = null,
)
