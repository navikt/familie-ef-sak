package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.ef.sak.domene.DokumentVariantformat
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
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

    fun oppdaterJournalpost(oppdaterJournalpostRequest: OppdaterJournalpostRequest,
                            journalpostId: String): OppdaterJournalpostResponse {
        return putForEntity<Ressurs<OppdaterJournalpostResponse>>(URI.create("${dokarkivUri}/v2/${journalpostId}"),
                                                                  oppdaterJournalpostRequest).data
               ?: error("Kunne ikke oppdatere journalpost med id $journalpostId")
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