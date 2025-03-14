package no.nav.familie.ef.sak.journalføring

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggle
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.journalføring.dto.DokumentVariantformat
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.http.client.RessursException
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
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavHttpHeaders
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class JournalpostClient(
    @Qualifier("azure") restOperations: RestOperations,
    integrasjonerConfig: IntegrasjonerConfig,
    private val featureToggleService: FeatureToggleService,
) : AbstractPingableRestClient(restOperations, "journalpost") {
    override val pingUri: URI = integrasjonerConfig.pingUri
    private val journalpostURI: URI = integrasjonerConfig.journalPostUri
    private val dokarkivUri: URI = integrasjonerConfig.dokarkivUri

    fun finnJournalposter(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        kastApiFeilDersomUtviklerMedVeilederrolle()
        return postForEntity<Ressurs<List<Journalpost>>>(journalpostURI, journalposterForBrukerRequest).data
            ?: error("Kunne ikke hente vedlegg for ${journalposterForBrukerRequest.brukerId.id}")
    }

    fun finnJournalposterForBrukerOgTema(journalposterForBrukerOgTemaRequest: JournalposterForVedleggRequest): List<Journalpost> {
        kastApiFeilDersomUtviklerMedVeilederrolle()
        return postForEntity<Ressurs<List<Journalpost>>>(
            URI.create("$journalpostURI/temaer"),
            journalposterForBrukerOgTemaRequest,
        ).data
            ?: error("Kunne ikke hente vedlegg for ${journalposterForBrukerOgTemaRequest.brukerId.id}")
    }

    fun hentJournalpost(journalpostId: String): Journalpost {
        kastApiFeilDersomUtviklerMedVeilederrolle()
        val ressurs =
            try {
                getForEntity<Ressurs<Journalpost>>(URI.create("$journalpostURI?journalpostId=$journalpostId"))
            } catch (e: RessursException) {
                if (e.message?.contains("Fant ikke journalpost i fagarkivet") == true) {
                    throw ApiFeil("Finner ikke journalpost i fagarkivet", BAD_REQUEST)
                } else {
                    throw e
                }
            }
        return ressurs.getDataOrThrow()
    }

    private fun kastApiFeilDersomUtviklerMedVeilederrolle() {
        if (featureToggleService.isEnabled(FeatureToggle.UtviklerMedVeilederrolle)) {
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
        return getForEntity<Ressurs<ByteArray>>(
            UriComponentsBuilder
                .fromUriString(
                    "$journalpostURI/hentdokument/" +
                        "$journalpostId/$dokumentInfoId",
                ).queryParam("variantFormat", dokumentVariantformat)
                .build()
                .toUri(),
        ).getDataOrThrow()
    }

    fun hentOvergangsstønadSøknad(
        journalpostId: String,
        dokumentInfoId: String,
    ): SøknadOvergangsstønad {
        val data = getForEntity<Ressurs<ByteArray>>(jsonDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()
        return objectMapper.readValue(data)
    }

    fun hentBarnetilsynSøknad(
        journalpostId: String,
        dokumentInfoId: String,
    ): SøknadBarnetilsyn {
        val data = getForEntity<Ressurs<ByteArray>>(jsonDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()
        return objectMapper.readValue(data)
    }

    fun hentSkolepengerSøknad(
        journalpostId: String,
        dokumentInfoId: String,
    ): SøknadSkolepenger {
        val data = getForEntity<Ressurs<ByteArray>>(jsonDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()
        return objectMapper.readValue(data)
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
        putForEntity<Ressurs<OppdaterJournalpostResponse>>(
            URI.create("$dokarkivUri/v2/$journalpostId"),
            oppdaterJournalpostRequest,
            headerMedSaksbehandler(saksbehandler),
        ).data
            ?: error("Kunne ikke oppdatere journalpost med id $journalpostId")

    fun arkiverDokument(
        arkiverDokumentRequest: ArkiverDokumentRequest,
        saksbehandler: String?,
    ): ArkiverDokumentResponse =
        postForEntity<Ressurs<ArkiverDokumentResponse>>(
            URI.create("$dokarkivUri/v4"),
            arkiverDokumentRequest,
            headerMedSaksbehandler(saksbehandler),
        ).data
            ?: error("Kunne ikke arkivere dokument med fagsakid ${arkiverDokumentRequest.fagsakId}")

    fun ferdigstillJournalpost(
        journalpostId: String,
        journalførendeEnhet: String,
        saksbehandler: String?,
    ) {
        val ressurs =
            try {
                putForEntity<Ressurs<OppdaterJournalpostResponse>>(
                    URI.create("$dokarkivUri/v2/$journalpostId/ferdigstill?journalfoerendeEnhet=$journalførendeEnhet"),
                    "",
                    headerMedSaksbehandler(saksbehandler),
                )
            } catch (e: RessursException) {
                brukerfeilHvis(e.ressurs.melding.contains("DokumentInfo.tittel")) {
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
        putForEntity<Ressurs<String>>(
            URI.create("$dokarkivUri/dokument/$dokumentInfoId/logiskVedlegg"),
            request,
        ).data ?: error("Kunne ikke bulk oppdatere logiske vedlegg på dokument med id=$dokumentInfoId")
}
