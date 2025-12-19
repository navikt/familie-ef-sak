package no.nav.familie.ef.sak.behandlingsflyt.steg

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillBehandlingTask
import no.nav.familie.ef.sak.blankett.BlankettHelper.lagArkiverBlankettRequestMotNyLøsning
import no.nav.familie.ef.sak.blankett.BlankettService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.vedtak.TotrinnskontrollService
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException

@Service
class SaksbehandlingsblankettSteg(
    private val blankettService: BlankettService,
    private val taskService: TaskService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val journalpostClient: JournalpostClient,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
) : BehandlingSteg<Void?> {
    private val logger = Logg.getLogger(this::class)

    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        if (saksbehandling.erMigrering || saksbehandling.erMaskinellOmregning) {
            logger.info(
                "Oppretter ikke saksbehandlingsblankett for behandling=${saksbehandling.id}, " +
                    "behandling er migrering eller maskinell g-omregning",
            )
        } else {
            logger.info("Lag blankett for behandling=${saksbehandling.id}")
            val blankettPdf = blankettService.lagBlankett(saksbehandling.id)
            logger.info("Journalfører blankett for behandling=${saksbehandling.id}")
            journalførSaksbehandlingsblankett(saksbehandling, blankettPdf)
        }
        opprettFerdigstillBehandlingTask(saksbehandling)
    }

    private fun journalførSaksbehandlingsblankett(
        saksbehandling: Saksbehandling,
        blankettPdf: ByteArray,
    ) {
        val arkiverDokumentRequest = opprettArkiverDokumentRequest(saksbehandling, blankettPdf)
        val beslutter = totrinnskontrollService.hentBeslutter(saksbehandling.id)

        val journalpostId =
            try {
                journalpostClient.arkiverDokument(arkiverDokumentRequest, beslutter).journalpostId
            } catch (e: RessursException) {
                if (e.cause is HttpClientErrorException.Conflict) {
                    finnJournalpostIdForBlankett(saksbehandling)
                } else {
                    throw e
                }
            }

        behandlingService.leggTilBehandlingsjournalpost(
            journalpostId,
            Journalposttype.N,
            saksbehandling.id,
        )
    }

    private fun finnJournalpostIdForBlankett(saksbehandling: Saksbehandling): String {
        val journalposterForBruker =
            journalpostClient.finnJournalposter(
                JournalposterForBrukerRequest(
                    brukerId =
                        Bruker(
                            id = saksbehandling.ident,
                            type = BrukerIdType.FNR,
                        ),
                    antall = 100,
                    tema = listOf(Tema.ENF),
                    journalposttype = listOf(Journalposttype.N),
                ),
            )

        val forventetEksternReferanseId = "${saksbehandling.id}-blankett"

        return journalposterForBruker.single { it.eksternReferanseId == forventetEksternReferanseId }.journalpostId
    }

    private fun opprettArkiverDokumentRequest(
        saksbehandling: Saksbehandling,
        blankettPdf: ByteArray,
    ): ArkiverDokumentRequest {
        val fagsak = fagsakService.hentFagsak(saksbehandling.fagsakId)
        val personIdent = fagsak.hentAktivIdent()
        val enhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent)
        return lagArkiverBlankettRequestMotNyLøsning(
            personIdent,
            blankettPdf,
            enhet,
            fagsak.eksternId,
            saksbehandling.id,
            fagsak.stønadstype,
        )
    }

    override fun stegType(): StegType = StegType.LAG_SAKSBEHANDLINGSBLANKETT

    private fun opprettFerdigstillBehandlingTask(saksbehandling: Saksbehandling) {
        taskService.save(FerdigstillBehandlingTask.opprettTask(saksbehandling))
    }
}
