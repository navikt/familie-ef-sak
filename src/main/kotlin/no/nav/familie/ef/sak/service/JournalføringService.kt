package no.nav.familie.ef.sak.service;

import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.springframework.stereotype.Service;

@Service
class JournalføringService (private val journalpostClient: JournalpostClient) {

    fun hentJournalpost(journalpostId:String): Journalpost {
        return journalpostClient.hentJournalpost(journalpostId)
    }

    fun hentDokument(journalpostId:String, dokumentInfoId: String): ByteArray {
        return journalpostClient.hentDokument(journalpostId, dokumentInfoId)
    }
}
