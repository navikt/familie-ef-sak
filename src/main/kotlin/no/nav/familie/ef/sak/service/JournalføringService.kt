package no.nav.familie.ef.sak.service;

import com.fasterxml.jackson.core.JsonProcessingException
import no.nav.familie.ef.sak.api.journalføring.JournalføringRequest
import no.nav.familie.ef.sak.domene.DokumentBrevkode
import no.nav.familie.ef.sak.domene.DokumentVariantformat
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import no.nav.familie.kontrakter.felles.dokarkiv.*
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class JournalføringService(private val journalpostClient: JournalpostClient,
                           private val behandlingService: BehandlingService,
                           private val oppgaveService: OppgaveService) {

    private val logger = LoggerFactory.getLogger(JournalføringService::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

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

        val journalpost = hentJournalpost(journalpostId)

        oppdaterJournalpost(journalpost, journalføringRequest.dokumentTitler, journalføringRequest.fagsakId)


        oppgaveService.ferdigstillOppgave(journalføringRequest.oppgaveId.toLong())

        settSøknadPåBehandling(journalpostId)

        knyttJournalpostTilBehandling(journalpostId, behandling)

        // TODO: Spør Mirja - ny oppgave: EnhetId og Tilordnet til?

        return oppgaveService.opprettOppgave(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.BehandleSak,
                fristForFerdigstillelse = LocalDate.now().plusDays(2)
        )

    }

    private fun knyttJournalpostTilBehandling(journalpostId: String, behandling: Behandling) {
        behandlingService.oppdaterJournalpostIdPåBehandling(journalpostId, behandling)
    }

    private fun settSøknadPåBehandling(journalpostId: String) {
        hentJournalpost(journalpostId).dokumenter
                ?.filter { dokument -> DokumentBrevkode.erGyldigBrevkode(dokument.brevkode) && harOriginalDokument(dokument) }
                ?.map { Pair(DokumentBrevkode.fraBrevkode(it.brevkode), hentDokument(journalpostId, it.dokumentInfoId)) }
                ?.forEach {
                    konverterTilSøknadsobjekt(it)
                }
    }

    private fun konverterTilSøknadsobjekt(it: Pair<DokumentBrevkode, ByteArray>) {
        try {
            when (it.first) {
                DokumentBrevkode.OVERGANGSSTØNAD -> {
                    objectMapper.readValue(it.second, SøknadOvergangsstønad::class.java)
                    // TODO: Bent må sette inn i databasen når domenemodellen er klar
                }
                DokumentBrevkode.BARNETILSYN -> {
                    objectMapper.readValue(it.second, SøknadBarnetilsyn::class.java)
                    // TODO: Bent må sette inn i databasen når domenemodellen er klar
                }
                DokumentBrevkode.SKOLEPENGER -> {
                    objectMapper.readValue(it.second, SøknadSkolepenger::class.java)
                    // TODO: Bent må sette inn i databasen når domenemodellen er klar
                }
            }
        } catch (e: JsonProcessingException) {
            secureLogger.warn("Kan ikke konvertere journalpostDokument til søknadsobjekt", e)
            logger.warn("Kan ikke konvertere journalpostDokument til søknadsobjekt ${e.javaClass.simpleName}")
        }
    }

    private fun harOriginalDokument(dokument: no.nav.familie.kontrakter.felles.journalpost.DokumentInfo): Boolean =
            dokument.dokumentvarianter?.contains(Dokumentvariant(variantformat = DokumentVariantformat.ORIGINAL.toString()))
                    ?: false

    private fun oppdaterJournalpost(journalpost: Journalpost, dokumenttitler: Map<String, String>?, fagsakId: UUID) {
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
        journalpostClient.oppdaterJournalpost(oppdatertJournalpost, journalpost.journalpostId)
    }
}
