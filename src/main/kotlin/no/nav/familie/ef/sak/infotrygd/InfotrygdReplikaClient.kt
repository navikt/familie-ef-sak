package no.nav.familie.ef.sak.infotrygd

import efterlatte.prosessering.spring.TaskService
import no.nav.familie.ef.sak.infotrygd.skygge.SKYGGEKJØR_INFOTRYGD_TASK_TYPE
import no.nav.familie.ef.sak.infotrygd.skygge.SkyggeInfotrygdOperasjon
import no.nav.familie.ef.sak.infotrygd.skygge.SkyggeInfotrygdPayload
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.restklient.client.AbstractPingableRestClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class InfotrygdReplikaClient(
    @Value("\${INFOTRYGD_REPLIKA_API_URL}")
    private val infotrygdReplikaUri: URI,
    @Qualifier("azure")
    restOperations: RestOperations,
    private val taskService: TaskService,
    private val featureToggleService: FeatureToggleService,
) : AbstractPingableRestClient(restOperations, "infotrygd.replika") {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val perioderUri: URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaUri)
            .pathSegment("api/perioder")
            .build()
            .toUri()

    private val sammenslåttePerioderUri: URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaUri)
            .pathSegment("api/perioder/sammenslatte")
            .build()
            .toUri()

    private val finnSakerUri: URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaUri)
            .pathSegment("api/saker/finn")
            .build()
            .toUri()

    private val eksistererUri: URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaUri)
            .pathSegment("api/stonad/eksisterer")
            .build()
            .toUri()

    private fun migreringspersonerUri(antall: Int): URI =
        UriComponentsBuilder
            .fromUri(infotrygdReplikaUri)
            .pathSegment("api/perioder/migreringspersoner")
            .queryParam("antall", antall)
            .build()
            .toUri()

    fun hentPerioder(request: InfotrygdPeriodeRequest): InfotrygdPeriodeResponse {
        val response = postForEntity<InfotrygdPeriodeResponse>(perioderUri, request)
        skyggekjør(SkyggeInfotrygdOperasjon.HENT_PERIODER, request, response, request.personIdenter)
        return response
    }

    fun hentSammenslåttePerioder(request: InfotrygdPeriodeRequest): InfotrygdPeriodeResponse {
        val response = postForEntity<InfotrygdPeriodeResponse>(sammenslåttePerioderUri, request)
        skyggekjør(SkyggeInfotrygdOperasjon.HENT_SAMMENSLÅTTE_PERIODER, request, response, request.personIdenter)
        return response
    }

    fun hentSaker(request: InfotrygdSøkRequest): InfotrygdSakResponse {
        val response = postForEntity<InfotrygdSakResponse>(finnSakerUri, request)
        skyggekjør(SkyggeInfotrygdOperasjon.HENT_SAKER, request, response, request.personIdenter)
        return response
    }

    fun hentPersonerForMigrering(antall: Int): Set<String> {
        val response = getForEntity<Map<String, Any>>(migreringspersonerUri(antall))
        @Suppress("UNCHECKED_CAST")
        return (response.getValue("personIdenter") as List<String>).toSet()
    }

    /**
     * Infotrygd skal alltid returnere en stønadTreff for hver søknadType som er input
     */
    fun hentInslagHosInfotrygd(request: InfotrygdSøkRequest): InfotrygdFinnesResponse {
        require(request.personIdenter.isNotEmpty()) { "Identer har ingen verdier" }
        val response = postForEntity<InfotrygdFinnesResponse>(eksistererUri, request)
        skyggekjør(SkyggeInfotrygdOperasjon.HENT_INNSLAG_HOS_INFOTRYGD, request, response, request.personIdenter)
        return response
    }

    override val pingUri: URI
        get() =
            UriComponentsBuilder
                .fromUri(infotrygdReplikaUri)
                .pathSegment("api/ping")
                .build()
                .toUri()

    /**
     * Oppretter en asynkron task som gjør det samme kallet mot familie-ef-infotrygd-replika (GCP) og sammenligner
     * responsen med [forventetRespons] (svaret vi nettopp fikk fra familie-ef-infotrygd on-prem). Brukes til å
     * verifisere at migreringen til GCP-replikaen gir identiske svar, se
     * [no.nav.familie.ef.sak.infotrygd.skygge.SkyggekjørInfotrygdTask].
     *
     * Tasken opprettes med [TaskService.opprettIEgenTransaksjon] - det finnes ingen forretningstransaksjon her å henge
     * outbox-garantien på (kallet skjer utenfor enhver transaksjon), og det skal det heller ikke gjøre: skyggekjøringen
     * skal aldri kunne påvirke det ordinære kallet mot on-prem, så eventuelle feil ved oppretting av skyggetasken logges her.
     */
    private fun skyggekjør(
        operasjon: SkyggeInfotrygdOperasjon,
        request: Any,
        forventetRespons: Any,
        personIdenter: Set<String>,
    ) {
        if (featureToggleService.isEnabled(Toggle.SKYGGEKJØR_INFOTRYGD)) {
            try {
                taskService.opprettIEgenTransaksjon(
                    type = SKYGGEKJØR_INFOTRYGD_TASK_TYPE,
                    payload =
                        SkyggeInfotrygdPayload(
                            operasjon = operasjon,
                            request = jsonMapper.writeValueAsString(request),
                            forventetRespons = jsonMapper.writeValueAsString(forventetRespons),
                            personIdenter = personIdenter,
                        ),
                )
            } catch (e: Exception) {
                logger.error("Klarte ikke å opprette skyggetask for $operasjon mot familie-ef-infotrygd-replika", e)
            }
        }
    }
}
