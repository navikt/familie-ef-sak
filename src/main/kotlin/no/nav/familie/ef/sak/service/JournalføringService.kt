package no.nav.familie.ef.sak.service;

import no.nav.familie.ef.sak.api.journalføring.JournalføringRequest
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.kontrakter.felles.dokarkiv.*
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service;
import java.time.LocalDate
import java.util.*

@Service
class JournalføringService(private val journalpostClient: JournalpostClient,
                           private val behandlingService: BehandlingService,
                           private val oppgaveService: OppgaveService) {

    fun hentJournalpost(journalpostId: String): Journalpost {
        return journalpostClient.hentJournalpost(journalpostId)
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String): ByteArray {
        return journalpostClient.hentDokument(journalpostId, dokumentInfoId)
    }

    fun fullførJournalpost(journalføringRequest: JournalføringRequest, journalpostId: String): Long {
        val behandling = journalføringRequest.behandling.behandlingsId?.let { behandlingService.hentBehandling(it) }
                ?: behandlingService.opprettBehandling(behandlingType = journalføringRequest.behandling.behandlingType!!,
                        fagsakId = journalføringRequest.fagsakId)

        oppdaterJournalpost(journalpostId, journalføringRequest.dokumentTitler, journalføringRequest.fagsakId)

        oppgaveService.ferdigstillOppgave(journalføringRequest.oppgaveId.toLong())

        // TODO: Hent søknad fra mottak?

        // TODO: Spør Mirja - ny oppgave: EnhetId og Tilordnet til?
        // TODO: AktørId mangler
        return oppgaveService.opprettOppgave(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.BehandleSak,
                fristForFerdigstillelse = LocalDate.now().plusDays(2)
        )
    }

    private fun oppdaterJournalpost(journalpostId: String, dokumenttitler: Map<String, String>?, fagsakId: UUID) {
        val journalpost = hentJournalpost(journalpostId)
        val oppdatertJournalpost = OppdaterJournalpostRequest(
                bruker = journalpost.bruker?.let {
                    DokarkivBruker(
                            idType = IdType.valueOf(it.type.toString()),
                            id = it.id
                    )
                },
                tema = journalpost.tema,
                behandlingstema = journalpost.behandlingstema,
                tittel = journalpost.tittel,
                journalfoerendeEnhet = journalpost.journalforendeEnhet,
                sak = Sak(
                        fagsakId = fagsakId.toString(),
                        fagsaksystem = "EF",
                        sakstype = "FAGSAK"
                ),
                dokumenter = dokumenttitler?.let {
                    journalpost.dokumenter?.map { dokumentInfo ->
                        DokumentInfo(
                                dokumentInfoId = dokumentInfo.dokumentInfoId,
                                tittel = dokumenttitler[dokumentInfo.dokumentInfoId]
                                        ?: dokumentInfo.tittel,
                                brevkode = dokumentInfo.brevkode
                        )
                    }
                }
        )
        journalpostClient.oppdaterJournalpost(oppdatertJournalpost, journalpostId)
    }
}
