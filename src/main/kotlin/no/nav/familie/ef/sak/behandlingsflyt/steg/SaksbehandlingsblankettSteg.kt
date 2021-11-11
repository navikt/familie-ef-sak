package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillBehandlingTask
import no.nav.familie.ef.sak.blankett.BlankettHelper.lagArkiverBlankettRequestMotNyLøsning
import no.nav.familie.ef.sak.blankett.BlankettService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SaksbehandlingsblankettSteg(private val blankettService: BlankettService,
                                  private val taskRepository: TaskRepository,
                                  private val arbeidsfordelingService: ArbeidsfordelingService,
                                  private val totrinnskontrollService: TotrinnskontrollService,
                                  private val journalpostClient: JournalpostClient,
                                  private val behandlingService: BehandlingService,
                                  private val fagsakService: FagsakService) : BehandlingSteg<Void?> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun utførSteg(behandling: Behandling, data: Void?) {
        val blankettPdf = blankettService.lagBlankett(behandling.id)
        if (skalJournalføreBlankett(behandling)) {
            logger.info("Journalfører blankett for behandling=${behandling.id}")
            journalførSaksbehandlingsblankett(behandling, blankettPdf)
        }
        opprettFerdigstillOppgave(behandling)
    }

    private fun skalJournalføreBlankett(behandling: Behandling): Boolean =
            behandling.type == BehandlingType.FØRSTEGANGSBEHANDLING

    private fun journalførSaksbehandlingsblankett(behandling: Behandling, blankettPdf: ByteArray) {
        val arkiverDokumentRequest = opprettArkiverDokumentRequest(behandling, blankettPdf)
        val beslutter = totrinnskontrollService.hentBeslutter(behandling.id)

        val journalpostRespons = journalpostClient.arkiverDokument(arkiverDokumentRequest, beslutter)

        behandlingService.leggTilBehandlingsjournalpost(journalpostRespons.journalpostId, Journalposttype.N, behandling.id)
    }

    private fun opprettArkiverDokumentRequest(behandling: Behandling,
                                              blankettPdf: ByteArray): ArkiverDokumentRequest {
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val personIdent = fagsak.hentAktivIdent()
        val enhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent)
        return lagArkiverBlankettRequestMotNyLøsning(personIdent, blankettPdf, enhet, fagsak.eksternId.id)
    }

    override fun stegType(): StegType {
        return StegType.LAG_SAKSBEHANDLINGSBLANKETT
    }

    fun opprettFerdigstillOppgave(behandling: Behandling) {
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(behandling))
    }

}