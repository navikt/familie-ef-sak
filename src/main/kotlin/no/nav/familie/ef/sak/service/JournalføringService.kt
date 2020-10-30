package no.nav.familie.ef.sak.service;

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.familie.ef.sak.api.journalføring.JournalføringRequest
import no.nav.familie.ef.sak.domene.DokumentVariantformat
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.dokarkiv.*
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class JournalføringService(private val journalpostClient: JournalpostClient,
                           private val behandlingService: BehandlingService,
                           private val fagsakService: FagsakService,
                           private val oppgaveService: OppgaveService) {

    private val logger = LoggerFactory.getLogger(JournalføringService::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentJournalpost(journalpostId: String): Journalpost {
        return journalpostClient.hentJournalpost(journalpostId)
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String, dokumentVariantformat: DokumentVariantformat = DokumentVariantformat.ARKIV ): ByteArray {
        return journalpostClient.hentDokument(journalpostId, dokumentInfoId, dokumentVariantformat)
    }

    @Transactional
    fun fullførJournalpost(journalføringRequest: JournalføringRequest, journalpostId: String, journalførendeEnhet: String): Long {
        val behandling = hentBehandling(journalføringRequest)
        val journalpost = hentJournalpost(journalpostId)


        //sende eksternIdFagsak til joark

        val eksternFagsakId = fagsakService.hentEksternId(journalføringRequest.fagsakId)

        oppdaterJournalpost(journalpost, journalføringRequest.dokumentTitler, eksternFagsakId)
        ferdigstillJournalføring(journalpostId, journalførendeEnhet)
        ferdigstillJournalføringsoppgave(journalføringRequest)

        settSøknadPåBehandling(journalpostId, behandling.fagsakId, behandling.id)
        knyttJournalpostTilBehandling(journalpost, behandling)

        return opprettSaksbehandlingsoppgave(behandling)

    }

    private fun ferdigstillJournalføring(journalpostId: String, journalførendeEnhet: String) {
        journalpostClient.ferdigstillJournalpost(journalpostId, journalførendeEnhet)
    }

    private fun opprettSaksbehandlingsoppgave(behandling: Behandling): Long {
        // TODO: Spør Mirja - ny oppgave: skal EnhetId settes?
        return oppgaveService.opprettOppgave(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.BehandleSak,
                fristForFerdigstillelse = LocalDate.now().plusDays(2)
        )
    }

    private fun ferdigstillJournalføringsoppgave(journalføringRequest: JournalføringRequest) {
        oppgaveService.ferdigstillOppgave(journalføringRequest.oppgaveId.toLong())
    }

    private fun hentBehandling(journalføringRequest: JournalføringRequest) = hentEksisterendeBehandling(journalføringRequest.behandling.behandlingsId)
            ?: opprettBehandlingMedBehandlingstype(journalføringRequest.behandling.behandlingstype, journalføringRequest.fagsakId)


    private fun opprettBehandlingMedBehandlingstype(behandlingsType: BehandlingType?, fagsakId: UUID) =
            behandlingService.opprettBehandling(behandlingType = behandlingsType!!,
                    fagsakId = fagsakId)

    private fun hentEksisterendeBehandling(behandlingsId: UUID?): Behandling? {
        return behandlingsId?.let { behandlingService.hentBehandling(it) }
    }

    private fun knyttJournalpostTilBehandling(journalpost: Journalpost, behandling: Behandling) {
        behandlingService.oppdaterJournalpostIdPåBehandling(journalpost, behandling)
    }

    private fun settSøknadPåBehandling(journalpostId: String, fagsakId: UUID, behandlingsId: UUID) {
        hentJournalpost(journalpostId).dokumenter
                ?.filter { dokument -> DokumentBrevkode.erGyldigBrevkode(dokument.brevkode) && harOriginalDokument(dokument) }
                ?.map { Pair(DokumentBrevkode.fraBrevkode(it.brevkode), hentDokument(journalpostId, it.dokumentInfoId, DokumentVariantformat.ORIGINAL)) }
                ?.forEach {
                    konverterTilSøknadsobjekt(it, fagsakId, behandlingsId, journalpostId)
                }
    }

    private fun konverterTilSøknadsobjekt(it: Pair<DokumentBrevkode, ByteArray>, fagsakId: UUID, behandlingsId: UUID, journalpostId: String) {
        try {
            when (it.first) {
                DokumentBrevkode.OVERGANGSSTØNAD -> {
                    val søknad = objectMapper.readValue(it.second, SøknadOvergangsstønad::class.java)
                    behandlingService.mottaSøknadForOvergangsstønad(søknad, behandlingsId, fagsakId, journalpostId)
                }
                DokumentBrevkode.BARNETILSYN -> {
                    val søknad = objectMapper.readValue(it.second, SøknadBarnetilsyn::class.java)
                    behandlingService.mottaSøknadForBarnetilsyn(søknad, behandlingsId, fagsakId, journalpostId)
                }
                DokumentBrevkode.SKOLEPENGER -> {
                    val søknad = objectMapper.readValue(it.second, SøknadSkolepenger::class.java)
                    behandlingService.mottaSøknadForSkolepenger(søknad, behandlingsId, fagsakId, journalpostId)
                }
            }
        } catch (e: JsonProcessingException) {
            secureLogger.error("Kan ikke konvertere journalpostDokument til søknadsobjekt", e)
            logger.error("Kan ikke konvertere journalpostDokument til søknadsobjekt ${e.javaClass.simpleName}")
        }
    }

    private fun harOriginalDokument(dokument: no.nav.familie.kontrakter.felles.journalpost.DokumentInfo): Boolean =
            dokument.dokumentvarianter?.contains(Dokumentvariant(variantformat = DokumentVariantformat.ORIGINAL.toString()))
                    ?: false

    private fun oppdaterJournalpost(journalpost: Journalpost, dokumenttitler: Map<String, String>?, eksternFagsakId: Long) {
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
                        fagsakId = eksternFagsakId.toString(),
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
        journalpostClient.oppdaterJournalpost(oppdatertJournalpost, journalpost.journalpostId)
    }

}
