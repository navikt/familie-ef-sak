package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.service.ArbeidsfordelingService
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.steg.BehandlingSteg
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.task.FerdigstillBehandlingTask
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.FilType
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service

@Service
class BlankettSteg(
        private val behandlingService: BehandlingService,
        private val behandlingRepository: BehandlingRepository,
        private val journalpostClient: JournalpostClient,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val blankettRepository: BlankettRepository,
        private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${behandling.steg}")
        }
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {

        val journalpostForBehandling = journalpostClient.hentJournalpost(behandling.journalposter.first().journalpostId)
        val personIdent = behandlingRepository.finnAktivIdent(behandling.id)
        val enhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent)
        val blankettPdf = blankettRepository.findByIdOrThrow(behandling.id).pdf.bytes

        val arkiverDokumentRequest = lagArkiverBlankettRequest(personIdent, blankettPdf, enhet, journalpostForBehandling.sak?.fagsakId)
        val journalpostRespons = journalpostClient.arkiverDokument(arkiverDokumentRequest)
        behandlingService.oppdaterJournalpostIdPåBehandling(journalpostRespons.journalpostId, Journalposttype.N, behandling)

        ferdigstillBehandling(behandling)
    }

    private fun lagArkiverBlankettRequest(personIdent: String,
                                          pdf: ByteArray,
                                          enhet: String,
                                          fagsakId: String?): ArkiverDokumentRequest {
        return ArkiverDokumentRequest(
                fnr = personIdent,
                forsøkFerdigstill = true,
                hoveddokumentvarianter = listOf(Dokument(pdf,
                                                         FilType.PDFA,
                                                         null,
                                                         "Blankett for overgangsstønad",
                                                         "OVERGANGSSTØNAD_BLANKETT")),
                vedleggsdokumenter = listOf(),
                fagsakId = fagsakId,
                journalførendeEnhet = enhet)
    }

    private fun ferdigstillBehandling(behandling: Behandling) {
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(behandling))
    }

    override fun stegType(): StegType = StegType.JOURNALFØR_BLANKETT


}