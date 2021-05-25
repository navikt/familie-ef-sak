package no.nav.familie.ef.sak.integration

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.domene.DokumentVariantformat
import no.nav.familie.http.client.AbstractPingableRestClient
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
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI


@Component
class JournalpostClient(@Qualifier("azure") restOperations: RestOperations,
                        integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "oppgave") {


    override val pingUri: URI = integrasjonerConfig.pingUri
    private val journalpostURI: URI = integrasjonerConfig.journalPostUri
    private val dokarkivUri: URI = integrasjonerConfig.dokarkivUri

    fun hentJournalpost(journalpostId: String): Journalpost {
        return getForEntity<Ressurs<Journalpost>>(URI.create("${journalpostURI}?journalpostId=${journalpostId}")).getDataOrThrow()
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String, dokumentVariantformat: DokumentVariantformat): ByteArray {
        return getForEntity<Ressurs<ByteArray>>(UriComponentsBuilder
                                                        .fromUriString("${journalpostURI}/hentdokument/" +
                                                                       "${journalpostId}/${dokumentInfoId}")
                                                        .queryParam("variantFormat", dokumentVariantformat)
                                                        .build()
                                                        .toUri())
                .getDataOrThrow()
    }

    fun hentOvergangsstønadSøknad(journalpostId: String, dokumentInfoId: String): SøknadOvergangsstønad {
        val data = getForEntity<Ressurs<ByteArray>>(jsonDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()
        return objectMapper.readValue<SøknadOvergangsstønad>(data)
    }

    fun hentBarnetilsynSøknad(journalpostId: String, dokumentInfoId: String): SøknadBarnetilsyn {
        val data = getForEntity<Ressurs<ByteArray>>(jsonDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()
        return objectMapper.readValue<SøknadBarnetilsyn>(data)
    }

    fun hentSkolepengerSøknad(journalpostId: String, dokumentInfoId: String): SøknadSkolepenger {
        val data = getForEntity<Ressurs<ByteArray>>(jsonDokumentUri(journalpostId, dokumentInfoId)).getDataOrThrow()
        return objectMapper.readValue<SøknadSkolepenger>(data)
    }

    private fun jsonDokumentUri(journalpostId: String, dokumentInfoId: String): URI {
        return UriComponentsBuilder
                .fromUri(journalpostURI)
                .pathSegment("hentdokument", journalpostId, dokumentInfoId)
                .queryParam("variantFormat", DokumentVariantformat.ORIGINAL)
                .build()
                .toUri()
    }

    fun oppdaterJournalpost(oppdaterJournalpostRequest: OppdaterJournalpostRequest,
                            journalpostId: String): OppdaterJournalpostResponse {
        return putForEntity<Ressurs<OppdaterJournalpostResponse>>(URI.create("${dokarkivUri}/v2/${journalpostId}"),
                                                                  oppdaterJournalpostRequest).data
               ?: error("Kunne ikke oppdatere journalpost med id $journalpostId")
    }

    fun arkiverDokument(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse {
        return postForEntity<Ressurs<ArkiverDokumentResponse>>(URI.create("${dokarkivUri}/v4/"),
                                                                  arkiverDokumentRequest).data
               ?: error("Kunne ikke arkivere dokument med fagsakid ${arkiverDokumentRequest.fagsakId}")
    }

    fun ferdigstillJournalpost(journalpostId: String, journalførendeEnhet: String) {
        val ressurs = putForEntity<Ressurs<OppdaterJournalpostResponse>>(
                URI.create("${dokarkivUri}/v2/${journalpostId}/ferdigstill?journalfoerendeEnhet=${journalførendeEnhet}"),
                "")

        if (ressurs.status != Ressurs.Status.SUKSESS) {
            secureLogger.error(" Feil ved oppdatering av journalpost=${journalpostId} - mottok: $ressurs")
            error("Feil ved oppdatering av journalpost=$journalpostId")
        }

    }
}