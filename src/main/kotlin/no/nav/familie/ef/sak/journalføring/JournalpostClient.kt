package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.journalføring.dto.DokumentVariantformat
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.BulkOppdaterLogiskVedleggRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.log.NavHttpHeaders
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class JournalpostClient(
    @Qualifier("integrasjonerRestClient") private val restClient: RestClient,
    integrasjonerConfig: IntegrasjonerConfig,
    private val featureToggleService: FeatureToggleService,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")
    private val journalpostURI: URI = integrasjonerConfig.journalPostUri
    private val dokarkivUri: URI = integrasjonerConfig.dokarkivUri

    fun finnJournalposter(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        kastApiFeilDersomUtviklerMedVeilederrolle()
        return restClient
            .post()
            .uri(journalpostURI)
            .body(journalposterForBrukerRequest)
            .retrieve()
            .body<Ressurs<List<Journalpost>>>()!!
            .data
            ?: error("Kunne ikke hente vedlegg for ${journalposterForBrukerRequest.brukerId.id}")
    }

    fun finnJournalposterForBrukerOgTema(journalposterForBrukerOgTemaRequest: JournalposterForVedleggRequest): List<Journalpost> {
        kastApiFeilDersomUtviklerMedVeilederrolle()
        return restClient
            .post()
            .uri(URI.create("$journalpostURI/temaer"))
            .body(journalposterForBrukerOgTemaRequest)
            .retrieve()
            .body<Ressurs<List<Journalpost>>>()!!
            .data
            ?: error("Kunne ikke hente vedlegg for ${journalposterForBrukerOgTemaRequest.brukerId.id}")
    }

    fun hentJournalpost(journalpostId: String): Journalpost {
        kastApiFeilDersomUtviklerMedVeilederrolle()
        val ressurs =
            try {
                restClient
                    .get()
                    .uri(URI.create("$journalpostURI?journalpostId=$journalpostId"))
                    .retrieve()
                    .body<Ressurs<Journalpost>>()!!
            } catch (e: HttpClientErrorException) {
                if (e.responseBodyAsString.contains("Fant ikke journalpost i fagarkivet")) {
                    throw ApiFeil("Finner ikke journalpost i fagarkivet", BAD_REQUEST)
                } else {
                    throw e
                }
            }
        return ressurs.getDataOrThrow()
    }

    private fun kastApiFeilDersomUtviklerMedVeilederrolle() {
        if (featureToggleService.isEnabled(Toggle.UTVIKLER_MED_VEILEDERRROLLE)) {
            throw ApiFeil(
                "Kan ikke hente ut journalposter som utvikler med veilederrolle. Kontakt teamet dersom du har saksbehandlerrolle.",
                FORBIDDEN,
            )
        }
    }

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
        dokumentVariantformat: DokumentVariantformat,
    ): ByteArray {
        kastApiFeilDersomUtviklerMedVeilederrolle()
        return restClient
            .get()
            .uri(
                UriComponentsBuilder
                    .fromUriString(
                        "$journalpostURI/hentdokument/" +
                            "$journalpostId/$dokumentInfoId",
                    ).queryParam("variantFormat", dokumentVariantformat)
                    .build()
                    .toUri(),
            ).retrieve()
            .body<Ressurs<ByteArray>>()!!
            .getDataOrThrow()
    }

    fun hentOvergangsstønadSøknad(
        journalpostId: String,
        dokumentInfoId: String,
    ): SøknadOvergangsstønad {
        val data =
            restClient
                .get()
                .uri(jsonDokumentUri(journalpostId, dokumentInfoId))
                .retrieve()
                .body<Ressurs<ByteArray>>()!!
                .getDataOrThrow()
        return jsonMapper.readValue(data)
    }

    fun hentBarnetilsynSøknad(
        journalpostId: String,
        dokumentInfoId: String,
    ): SøknadBarnetilsyn {
        val data =
            restClient
                .get()
                .uri(jsonDokumentUri(journalpostId, dokumentInfoId))
                .retrieve()
                .body<Ressurs<ByteArray>>()!!
                .getDataOrThrow()
        return jsonMapper.readValue(data)
    }

    fun hentSkolepengerSøknad(
        journalpostId: String,
        dokumentInfoId: String,
    ): SøknadSkolepenger {
        val data =
            restClient
                .get()
                .uri(jsonDokumentUri(journalpostId, dokumentInfoId))
                .retrieve()
                .body<Ressurs<ByteArray>>()!!
                .getDataOrThrow()
        return jsonMapper.readValue(data)
    }

    private fun jsonDokumentUri(
        journalpostId: String,
        dokumentInfoId: String,
    ): URI =
        UriComponentsBuilder
            .fromUri(journalpostURI)
            .pathSegment("hentdokument", journalpostId, dokumentInfoId)
            .queryParam("variantFormat", DokumentVariantformat.ORIGINAL)
            .build()
            .toUri()

    fun oppdaterJournalpost(
        oppdaterJournalpostRequest: OppdaterJournalpostRequest,
        journalpostId: String,
        saksbehandler: String?,
    ): OppdaterJournalpostResponse =
        restClient
            .put()
            .uri(URI.create("$dokarkivUri/v2/$journalpostId"))
            .headers { it.addAll(headerMedSaksbehandler(saksbehandler)) }
            .body(oppdaterJournalpostRequest)
            .retrieve()
            .body<Ressurs<OppdaterJournalpostResponse>>()!!
            .data
            ?: error("Kunne ikke oppdatere journalpost med id $journalpostId")

    fun arkiverDokument(
        arkiverDokumentRequest: ArkiverDokumentRequest,
        saksbehandler: String?,
    ): ArkiverDokumentResponse =
        restClient
            .post()
            .uri(URI.create("$dokarkivUri/v4"))
            .headers { it.addAll(headerMedSaksbehandler(saksbehandler)) }
            .body(arkiverDokumentRequest)
            .retrieve()
            .body<Ressurs<ArkiverDokumentResponse>>()!!
            .data
            ?: error("Kunne ikke arkivere dokument med fagsakid ${arkiverDokumentRequest.fagsakId}")

    fun ferdigstillJournalpost(
        journalpostId: String,
        journalførendeEnhet: String,
        saksbehandler: String?,
    ) {
        val ressurs =
            try {
                restClient
                    .put()
                    .uri(URI.create("$dokarkivUri/v2/$journalpostId/ferdigstill?journalfoerendeEnhet=$journalførendeEnhet"))
                    .headers { it.addAll(headerMedSaksbehandler(saksbehandler)) }
                    .body("")
                    .retrieve()
                    .body<Ressurs<OppdaterJournalpostResponse>>()!!
            } catch (e: HttpClientErrorException) {
                brukerfeilHvis(e.responseBodyAsString.contains("DokumentInfo.tittel")) {
                    "Mangler tittel på et/flere dokument/vedlegg"
                }
                throw e
            }

        if (ressurs.status != Ressurs.Status.SUKSESS) {
            secureLogger.error(" Feil ved oppdatering av journalpost=$journalpostId - mottok: $ressurs")
            error("Feil ved oppdatering av journalpost=$journalpostId")
        }
    }

    private fun headerMedSaksbehandler(saksbehandler: String?): HttpHeaders {
        val httpHeaders = HttpHeaders()
        if (saksbehandler != null) {
            httpHeaders.set(NavHttpHeaders.NAV_USER_ID.asString(), saksbehandler)
        }
        return httpHeaders
    }

    fun oppdaterLogiskeVedlegg(
        dokumentInfoId: String,
        request: BulkOppdaterLogiskVedleggRequest,
    ): String =
        restClient
            .put()
            .uri(URI.create("$dokarkivUri/dokument/$dokumentInfoId/logiskVedlegg"))
            .body(request)
            .retrieve()
            .body<Ressurs<String>>()!!
            .data ?: error("Kunne ikke bulk oppdatere logiske vedlegg på dokument med id=$dokumentInfoId")
}
