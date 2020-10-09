package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.config.IntegrasjonerConfig
import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI


@Component
class JournalpostClient(@Qualifier("azure") restOperations: RestOperations,
                        private val integrasjonerConfig: IntegrasjonerConfig)
    : AbstractPingableRestClient(restOperations, "oppgave") {

    override val pingUri: URI = integrasjonerConfig.pingUri
    private val journalpostURI: URI = integrasjonerConfig.journalPostUri

    fun hentJournalpost(journalpostId: String): Journalpost {
        return getForEntity<Ressurs<Journalpost>>(URI.create("${journalpostURI}?journalpostId=${journalpostId}")).data!!
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String): ByteArray {
        return getForEntity<Ressurs<ByteArray>>(URI.create("${journalpostURI}/hentdokument/${journalpostId}/${dokumentInfoId}")).data!!
    }
}