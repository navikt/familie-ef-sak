package no.nav.familie.ef.sak.journalføring

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.infrastruktur.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.journalføring.dto.DokumentVariantformat
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.webflux.client.AbstractPingableWebClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class JournalpostClient(
    @Qualifier("azureWebClient") webClient: WebClient,
    integrasjonerConfig: IntegrasjonerConfig
) :
    AbstractPingableWebClient(webClient, "journalpost") {

    override val pingUri: URI = integrasjonerConfig.pingUri
    private val journalpostURI: URI = integrasjonerConfig.journalPostUri
    private val dokarkivUri: URI = integrasjonerConfig.dokarkivUri

    fun finnJournalposter(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        return postForEntity<Ressurs<List<Journalpost>>>(journalpostURI, journalposterForBrukerRequest).data
            ?: error("Kunne ikke hente vedlegg for ${journalposterForBrukerRequest.brukerId.id}")
    }

    fun hentJournalpost(journalpostId: String): Journalpost {
        val ressurs = try {
            getForEntity<Ressurs<Journalpost>>(URI.create("$journalpostURI?journalpostId=$journalpostId"))
        } catch (e: RessursException) {
            if (e.message?.contains("Fant ikke journalpost i fagarkivet") == true) {
                throw ApiFeil("Finner ikke journalpost i fagarkivet", HttpStatus.BAD_REQUEST)
            } else {
                throw e
            }
        }
        return ressurs.getDataOrThrow()
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String, dokumentVariantformat: DokumentVariantformat): ByteArray {
        return getForEntity<Ressurs<ByteArray>>(
            UriComponentsBuilder
                .fromUriString(
                    "$journalpostURI/hentdokument/" +
                        "$journalpostId/$dokumentInfoId"
                )
                .queryParam("variantFormat", dokumentVariantformat)
                .build()
                .toUri()
        )
            .getDataOrThrow()
    }

    fun hentOvergangsstønadSøknad(journalpostId: String, dokumentInfoId: String): SøknadOvergangsstønad {
        val data = getForEntity<Ressurs<ByteArray>>(jsonDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()
        return objectMapper.readValue(data)
    }

    fun hentBarnetilsynSøknad(journalpostId: String, dokumentInfoId: String): SøknadBarnetilsyn {
        val data = getForEntity<Ressurs<ByteArray>>(jsonDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()
        return objectMapper.readValue(data)
    }

    fun hentSkolepengerSøknad(journalpostId: String, dokumentInfoId: String): SøknadSkolepenger {
        val data = getForEntity<Ressurs<ByteArray>>(jsonDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()
        return objectMapper.readValue(data)
    }

    private fun jsonDokumentUri(journalpostId: String, dokumentInfoId: String): URI {
        return UriComponentsBuilder
            .fromUri(journalpostURI)
            .pathSegment("hentdokument", journalpostId, dokumentInfoId)
            .queryParam("variantFormat", DokumentVariantformat.ORIGINAL)
            .build()
            .toUri()
    }

    fun oppdaterJournalpost(
        oppdaterJournalpostRequest: OppdaterJournalpostRequest,
        journalpostId: String,
        saksbehandler: String?
    ): OppdaterJournalpostResponse {
        return putForEntity<Ressurs<OppdaterJournalpostResponse>>(
            URI.create("$dokarkivUri/v2/$journalpostId"),
            oppdaterJournalpostRequest,
            headerMedSaksbehandler(saksbehandler)
        ).data
            ?: error("Kunne ikke oppdatere journalpost med id $journalpostId")
    }

    fun arkiverDokument(arkiverDokumentRequest: ArkiverDokumentRequest, saksbehandler: String?): ArkiverDokumentResponse {
        return postForEntity<Ressurs<ArkiverDokumentResponse>>(
            URI.create("$dokarkivUri/v4/"),
            arkiverDokumentRequest,
            headerMedSaksbehandler(saksbehandler)
        ).data
            ?: error("Kunne ikke arkivere dokument med fagsakid ${arkiverDokumentRequest.fagsakId}")
    }

    fun ferdigstillJournalpost(journalpostId: String, journalførendeEnhet: String, saksbehandler: String?) {
        val ressurs = try {
            putForEntity<Ressurs<OppdaterJournalpostResponse>>(
                URI.create("$dokarkivUri/v2/$journalpostId/ferdigstill?journalfoerendeEnhet=$journalførendeEnhet"),
                "",
                headerMedSaksbehandler(saksbehandler)
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
}
