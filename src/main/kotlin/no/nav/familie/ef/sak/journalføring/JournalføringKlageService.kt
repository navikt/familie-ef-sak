package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.journalføring.JournalføringHelper.validerGyldigAvsender
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequestV2
import no.nav.familie.ef.sak.journalføring.dto.Journalføringsårsak
import no.nav.familie.ef.sak.klage.KlageService
import no.nav.familie.ef.sak.klage.dto.OpprettKlageDto
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class JournalføringKlageService(
    private val fagsakService: FagsakService,
    private val oppgaveService: OppgaveService,
    private val journalpostService: JournalpostService,
    private val klageService: KlageService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun fullførJournalpostV2(
        journalføringRequest: JournalføringRequestV2,
        journalpost: Journalpost,
    ) {
        validerGyldigAvsender(journalpost, journalføringRequest)

        return if (journalføringRequest.skalJournalføreTilNyBehandling()) {
            journalførKlageTilNyBehandling(journalføringRequest, journalpost)
        } else {
            journalførKlageTilEksisterendeBehandling(journalføringRequest, journalpost)
        }
    }

    private fun journalførKlageTilEksisterendeBehandling(
        journalføringRequest: JournalføringRequestV2,
        journalpost: Journalpost,
    ) {
        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(journalføringRequest.fagsakId)
        val nyAvsender =
            JournalføringHelper.utledNyAvsender(journalføringRequest.nyAvsender, journalpost.bruker)

        logger.info(
            "Journalfører journalpost=${journalpost.journalpostId} på eksisterende " +
                "fagsak=${fagsak.id} stønadstype=${fagsak.stønadstype} ",
        )

        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            dokumenttitler = journalføringRequest.dokumentTitler,
            journalførendeEnhet = journalføringRequest.journalførendeEnhet,
            fagsak = fagsak,
            logiskeVedlegg = journalføringRequest.logiskeVedlegg,
            saksbehandler = saksbehandler,
            nyAvsender = nyAvsender,
        )
        ferdigstillJournalføringsoppgave(journalføringRequest.oppgaveId.toLong())
    }

    private fun journalførKlageTilNyBehandling(
        journalføringRequest: JournalføringRequestV2,
        journalpost: Journalpost,
    ) {
        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(journalføringRequest.fagsakId)
        val mottattDato = journalføringRequest.mottattDato
        val nyAvsender =
            JournalføringHelper.utledNyAvsender(journalføringRequest.nyAvsender, journalpost.bruker)
        logger.info(
            "Journalfører journalpost=${journalpost.journalpostId} på ny klagebehandling på " +
                "fagsak=${fagsak.id} stønadstype=${fagsak.stønadstype} mottattDato=$mottattDato",
        )

        val klageMottatt = mottattDato ?: journalpost.datoMottatt?.toLocalDate()
        feilHvis(klageMottatt == null) {
            "Mangler dato mottatt"
        }

        val opprettKlageDto = OpprettKlageDto(klageMottatt, journalføringRequest.årsak == Journalføringsårsak.KLAGE_TILBAKEKREVING, Klagebehandlingsårsak.ORDINÆR)

        klageService.validerOgOpprettKlage(fagsak, opprettKlageDto)

        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            dokumenttitler = journalføringRequest.dokumentTitler,
            journalførendeEnhet = journalføringRequest.journalførendeEnhet,
            fagsak = fagsak,
            logiskeVedlegg = journalføringRequest.logiskeVedlegg,
            saksbehandler = saksbehandler,
            nyAvsender = nyAvsender,
        )

        ferdigstillJournalføringsoppgave(journalføringRequest.oppgaveId.toLong())
    }

    private fun ferdigstillJournalføringsoppgave(oppgaveId: Long) {
        oppgaveService.ferdigstillOppgave(oppgaveId)
    }
}
