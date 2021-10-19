package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlingSteg
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillBehandlingTask
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BlankettSteg(
        private val behandlingService: BehandlingService,
        private val behandlingRepository: BehandlingRepository,
        private val journalpostClient: JournalpostClient,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val blankettRepository: BlankettRepository,
        private val totrinnskontrollService: TotrinnskontrollService,
        private val taskRepository: TaskRepository) : BehandlingSteg<Void?> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun validerSteg(behandling: Behandling) {
        if (behandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${behandling.steg}")
        }
    }

    override fun utførSteg(behandling: Behandling, data: Void?) {
        val journalposter = behandlingService.hentBehandlingsjournalposter(behandling.id)
        val journalpostForBehandling = journalpostClient.hentJournalpost(journalposter.first().journalpostId)
        val personIdent = behandlingRepository.finnAktivIdent(behandling.id)
        val enhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent)
        val blankettPdf = blankettRepository.findByIdOrThrow(behandling.id).pdf.bytes
        val beslutter = totrinnskontrollService.hentBeslutter(behandling.id)
        if (beslutter == null) {
            logger.info("steg=${stegType()} fant ikke beslutter på behandling=$behandling")
        }

        val arkiverDokumentRequest =
                lagArkiverBlankettRequest(personIdent, blankettPdf, enhet, journalpostForBehandling.sak?.fagsakId)
        val journalpostRespons = journalpostClient.arkiverDokument(arkiverDokumentRequest, beslutter)
        behandlingService.leggTilBehandlingsjournalpost(journalpostRespons.journalpostId, Journalposttype.N, behandling.id)

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
                                                         Filtype.PDFA,
                                                         null,
                                                         "Blankett for overgangsstønad",
                                                         Dokumenttype.OVERGANGSSTØNAD_BLANKETT)),
                vedleggsdokumenter = listOf(),
                fagsakId = fagsakId,
                journalførendeEnhet = enhet)
    }

    private fun ferdigstillBehandling(behandling: Behandling) {
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(behandling))
    }

    override fun stegType(): StegType = StegType.JOURNALFØR_BLANKETT


}