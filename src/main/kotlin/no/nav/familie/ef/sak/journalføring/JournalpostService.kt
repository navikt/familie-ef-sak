package no.nav.familie.ef.sak.journalføring

import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.journalføring.dto.DokumentVariantformat
import no.nav.familie.ef.sak.journalføring.dto.OppdaterJournalpostMedDokumenterRequest
import no.nav.familie.ef.sak.opplysninger.personopplysninger.logger
import no.nav.familie.ef.sak.vedlegg.VedleggRequest
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import no.nav.familie.kontrakter.felles.Arkivtema
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.BulkOppdaterLogiskVedleggRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JournalpostService(
    private val journalpostClient: JournalpostClient,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentJournalpost(journalpostId: String): Journalpost = journalpostClient.hentJournalpost(journalpostId)

    fun finnJournalposter(
        personIdent: String,
        antall: Int = 20,
        typer: List<Journalposttype> = Journalposttype.values().toList(),
    ): List<Journalpost> =
        journalpostClient.finnJournalposter(
            JournalposterForBrukerRequest(
                brukerId =
                    Bruker(
                        id = personIdent,
                        type = BrukerIdType.FNR,
                    ),
                antall = antall,
                tema = listOf(Tema.ENF),
                journalposttype = typer,
            ),
        )

    fun finnJournalposterForVedleggRequest(
        personIdent: String,
        vedleggRequest: VedleggRequest,
    ): List<Journalpost> =
        journalpostClient.finnJournalposterForBrukerOgTema(
            JournalposterForVedleggRequest(
                brukerId =
                    Bruker(
                        id = personIdent,
                        type = BrukerIdType.FNR,
                    ),
                tema = vedleggRequest.tema,
                dokumenttype = vedleggRequest.dokumenttype,
                journalpostStatus = vedleggRequest.journalpostStatus,
                antall = 10000,
            ),
        )

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
        dokumentVariantformat: DokumentVariantformat = DokumentVariantformat.ARKIV,
    ): ByteArray = journalpostClient.hentDokument(journalpostId, dokumentInfoId, dokumentVariantformat)

    fun hentSøknadFraJournalpostForOvergangsstønad(journalpost: Journalpost): SøknadOvergangsstønad {
        val dokumentinfo = JournalføringHelper.plukkUtOriginaldokument(journalpost, DokumentBrevkode.OVERGANGSSTØNAD)
        return journalpostClient.hentOvergangsstønadSøknad(journalpost.journalpostId, dokumentinfo.dokumentInfoId)
    }

    fun hentSøknadFraJournalpostForBarnetilsyn(journalpost: Journalpost): SøknadBarnetilsyn {
        val dokumentinfo = JournalføringHelper.plukkUtOriginaldokument(journalpost, DokumentBrevkode.BARNETILSYN)
        return journalpostClient.hentBarnetilsynSøknad(journalpost.journalpostId, dokumentinfo.dokumentInfoId)
    }

    fun hentSøknadFraJournalpostForSkolepenger(journalpost: Journalpost): SøknadSkolepenger {
        val dokumentinfo = JournalføringHelper.plukkUtOriginaldokument(journalpost, DokumentBrevkode.SKOLEPENGER)
        return journalpostClient.hentSkolepengerSøknad(journalpost.journalpostId, dokumentinfo.dokumentInfoId)
    }

    fun oppdaterOgFerdigstillJournalpostMaskinelt(
        journalpost: Journalpost,
        journalførendeEnhet: String,
        fagsak: Fagsak,
    ) = oppdaterOgFerdigstillJournalpost(
        journalpost = journalpost,
        dokumenttitler = null,
        journalførendeEnhet = journalførendeEnhet,
        fagsak = fagsak,
        saksbehandler = null,
    )

    fun oppdaterDokumenterPåJournalpost(
        journalpost: Journalpost,
        request: OppdaterJournalpostMedDokumenterRequest,
    ) {
        validerDokumenterOgLogiskeVedlegg(request)
        if (request.logiskeVedlegg !== null) {
            oppdaterLogiskeVedlegg(journalpost, request.logiskeVedlegg)
        }
        if (request.dokumenttitler != null) {
            oppdaterJournalpostMedDokumenttitler(journalpost, request.dokumenttitler)
        }
    }

    fun oppdaterOgFerdigstillJournalpost(
        journalpost: Journalpost,
        dokumenttitler: Map<String, String>?,
        logiskeVedlegg: Map<String, List<LogiskVedlegg>>? = null,
        journalførendeEnhet: String,
        fagsak: Fagsak,
        saksbehandler: String?,
        nyAvsender: AvsenderMottaker? = null,
    ) {
        if (journalpost.journalstatus != Journalstatus.JOURNALFOERT) {
            oppdaterLogiskeVedlegg(journalpost, logiskeVedlegg)
            oppdaterJournalpostMedFagsakOgDokumenttitler(
                journalpost = journalpost,
                dokumenttitler = dokumenttitler,
                eksternFagsakId = fagsak.eksternId,
                saksbehandler = saksbehandler,
                nyAvsender = nyAvsender,
            )
            ferdigstillJournalføring(
                journalpostId = journalpost.journalpostId,
                journalførendeEnhet = journalførendeEnhet,
                saksbehandler = saksbehandler,
            )
        }
    }

    private fun oppdaterLogiskeVedlegg(
        journalpost: Journalpost,
        logiskeVedlegg: Map<String, List<LogiskVedlegg>>?,
    ) {
        // TODO: Fas ut kode som tilhører gammel løsning for journalføring
        // Skal ikke endre på logiske vedlegg dersom man journalfører fra gammel løsning. Gammel løsning sender ikke inn logiske vedlegg og vil derfor resultere i null her. Ny løsning vil sende inn tom liste.
        if (logiskeVedlegg == null) {
            return
        }

        journalpost.dokumenter?.forEach { dokument ->
            val eksisterendeLogiskeVedlegg = dokument.logiskeVedlegg ?: emptyList()
            val logiskeVedleggForDokument = logiskeVedlegg.get(dokument.dokumentInfoId) ?: emptyList()
            val harIdentiskInnhold =
                eksisterendeLogiskeVedlegg.containsAll(logiskeVedleggForDokument) && eksisterendeLogiskeVedlegg.size == logiskeVedleggForDokument.size
            if (!harIdentiskInnhold) {
                logger.info("oppdaterer logiske vedlegg på journalpost med id=${journalpost.journalpostId}")
                journalpostClient.oppdaterLogiskeVedlegg(
                    dokument.dokumentInfoId,
                    BulkOppdaterLogiskVedleggRequest(titler = logiskeVedleggForDokument.map { it.tittel }),
                )
            }
        }
    }

    private fun validerDokumenterOgLogiskeVedlegg(request: OppdaterJournalpostMedDokumenterRequest) {
        brukerfeilHvis(request.dokumenttitler == null && request.logiskeVedlegg == null) {
            "Mangler både dokumenttittler og logiske vedlegg i forbindelse med oppdatering av dokumenter til journalpost"
        }
        if (request.dokumenttitler != null) {
            brukerfeilHvis(request.dokumenttitler.containsValue("")) {
                "Kan ikke endre dokumenttittel til tom streng"
            }
            brukerfeilHvis(request.dokumenttitler.keys.contains("")) {
                "Mangler dokumentId på et eller flere dokumenter som skal endre tittel"
            }
        }
        if (request.logiskeVedlegg != null) {
            brukerfeilHvis(
                request.logiskeVedlegg.values
                    .flatten()
                    .any { it.tittel == "" },
            ) {
                "Kan ikke endre et eller flere logiske vedlegg til tom streng"
            }
            brukerfeilHvis(request.logiskeVedlegg.keys.contains("")) {
                "Mangler dokumentId på et eller flere logiske vedlegg"
            }
            brukerfeilHvis(
                request.logiskeVedlegg.values
                    .flatten()
                    .any { it.logiskVedleggId == "" },
            ) {
                "Mangler id på et eller flere logiske vedlegg"
            }
        }
    }

    private fun ferdigstillJournalføring(
        journalpostId: String,
        journalførendeEnhet: String,
        saksbehandler: String? = null,
    ) {
        journalpostClient.ferdigstillJournalpost(journalpostId, journalførendeEnhet, saksbehandler)
    }

    private fun oppdaterJournalpostMedFagsakOgDokumenttitler(
        journalpost: Journalpost,
        dokumenttitler: Map<String, String>?,
        eksternFagsakId: Long,
        saksbehandler: String?,
        nyAvsender: AvsenderMottaker?,
    ) {
        val oppdatertJournalpost =
            JournalføringHelper.lagOppdaterJournalpostRequest(journalpost, eksternFagsakId, dokumenttitler, nyAvsender)
        oppdaterJournalpost(oppdatertJournalpost, journalpost.journalpostId, saksbehandler)
    }

    private fun oppdaterJournalpostMedDokumenttitler(
        journalpost: Journalpost,
        dokumenttitler: Map<String, String>,
    ) {
        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val oppdatertJournalpost = JournalføringHelper.lagOppdaterJournalpostRequest(journalpost, dokumenttitler)
        oppdaterJournalpost(oppdatertJournalpost, journalpost.journalpostId, saksbehandler)
    }

    private fun oppdaterJournalpost(
        request: OppdaterJournalpostRequest,
        journalpostId: String,
        saksbehandler: String?,
    ) {
        try {
            journalpostClient.oppdaterJournalpost(request, journalpostId, saksbehandler)
        } catch (e: Exception) {
            secureLogger.error("Kunne ikke oppdatere journalpost med id=$journalpostId")
            throw e
        }
    }
}

data class JournalposterForVedleggRequest(
    val brukerId: Bruker,
    val tema: List<Arkivtema>?,
    val dokumenttype: String?,
    val journalpostStatus: String?,
    val antall: Int = 200,
)
