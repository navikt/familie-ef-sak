package no.nav.familie.ef.sak.service

import io.mockk.*
import no.nav.familie.ef.sak.api.journalføring.JournalføringBehandling
import no.nav.familie.ef.sak.api.journalføring.JournalføringRequest
import no.nav.familie.ef.sak.domene.DokumentVariantformat
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.journalpost.*
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class JournalføringServiceTest {

    private val journalpostClient: JournalpostClient = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val oppgaveService: OppgaveService = mockk()
    private val fagsakService: FagsakService = mockk()

    private val journalføringService = JournalføringService(journalpostClient, behandlingService, fagsakService, oppgaveService)

    val fagsakId = UUID.randomUUID()
    val fagsakEksternId = 12345L
    val journalpostId = "98765"
    val nyOppgaveId = 999999L
    val behandlingId = UUID.randomUUID()
    val oppgaveId = "1234567"
    val dokumentTitler = hashMapOf("12345" to "Asbjørns skilsmissepapirer", "23456" to "Eiriks samværsdokument")
    val dokumentInfoIdMedJsonVerdi = "12345"

    @BeforeEach
    fun setupMocks() {
        every { journalpostClient.hentJournalpost(journalpostId) } returns Journalpost(
                journalpostId = journalpostId,
                journalposttype = Journalposttype.I,
                journalstatus = Journalstatus.MOTTATT,
                tema = "ENF",
                behandlingstema = "ab0180",
                dokumenter = listOf(DokumentInfo(dokumentInfoIdMedJsonVerdi,
                                                 "Vedlegg1",
                                                 brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                                 dokumentvarianter = listOf(Dokumentvariant(variantformat = DokumentVariantformat.ORIGINAL.toString()),
                                                                            Dokumentvariant(variantformat = DokumentVariantformat.ARKIV.toString()))),
                                    DokumentInfo("99999", "Vedlegg2", brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                                 dokumentvarianter = listOf(Dokumentvariant(variantformat = DokumentVariantformat.ARKIV.toString()))),
                                    DokumentInfo("23456", "Vedlegg3", brevkode = "XYZ"),
                                    DokumentInfo("88888", "Vedlegg4", brevkode = "XYZ")),
                tittel = "Søknad om overgangsstønad"
        )

        every { fagsakService.hentEksternId(any()) } returns fagsakEksternId

        every { behandlingService.hentBehandling(behandlingId) } returns Behandling(id = behandlingId,
                                                                                    fagsakId = fagsakId,
                                                                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                                    status = BehandlingStatus.UTREDES,
                                                                                    steg = StegType.REGISTRERE_OPPLYSNINGER)

        every { behandlingService.opprettBehandling(any(), any()) } returns Behandling(id = behandlingId,
                                                                                       fagsakId = fagsakId,
                                                                                       type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                                       status = BehandlingStatus.UTREDES,
                                                                                       steg = StegType.REGISTRERE_OPPLYSNINGER)

        every { oppgaveService.ferdigstillOppgave(any()) } just runs
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any(), any()) } returns nyOppgaveId
        every { behandlingService.oppdaterJournalpostIdPåBehandling(any(), any()) } just runs
        every { journalpostClient.ferdigstillJournalpost(any(), any()) } just runs
    }

    @Test
    internal fun `skal fullføre manuell journalføring på eksisterende behandling`() {
        val slotDokumentInfoIder: MutableList<String> = mutableListOf<String>()
        val slotJournalpost = slot<OppdaterJournalpostRequest>()

        every {
            journalpostClient.oppdaterJournalpost(capture(slotJournalpost),
                                                  journalpostId)
        } returns OppdaterJournalpostResponse(
                journalpostId = journalpostId)

        every {
            journalpostClient.hentDokument(any(), capture(slotDokumentInfoIder), DokumentVariantformat.ORIGINAL)
        } returns objectMapper.writeValueAsString(Testsøknad.søknadOvergangsstønad).toByteArray()

        every {
            behandlingService.mottaSøknadForOvergangsstønad(any(), any(), any(), any())
        } just Runs

        val behandleSakOppgaveId = journalføringService.fullførJournalpost(
                journalpostId = journalpostId,
                journalføringRequest = JournalføringRequest(
                        dokumentTitler,
                        fagsakId,
                        oppgaveId,
                        JournalføringBehandling(behandlingsId = behandlingId),
                        "Z1234567",
                        "1234"
                ),
        )

        assertThat(behandleSakOppgaveId).isEqualTo(nyOppgaveId)
        assertThat(slotJournalpost.captured.sak?.fagsakId).isEqualTo(fagsakEksternId.toString())
        assertThat(slotJournalpost.captured.sak?.sakstype).isEqualTo("FAGSAK")
        assertThat(slotJournalpost.captured.sak?.fagsaksystem).isEqualTo("EF")
        dokumentTitler.forEach { (dokumentId, nyTittel) ->
            val oppdatertDokument =
                    slotJournalpost.captured.dokumenter?.find { dokument -> dokument.dokumentInfoId === dokumentId }
            assertThat(oppdatertDokument?.tittel).isEqualTo(nyTittel)
        }
        assertThat(slotDokumentInfoIder[0]).isEqualTo(dokumentInfoIdMedJsonVerdi)
        assertThat(slotDokumentInfoIder.size).isEqualTo(1)
        verify(exactly = 1) { behandlingService.mottaSøknadForOvergangsstønad(any(), any(), any(), any()) }
    }

    @Test
    internal fun `skal fullføre manuell journalføring på ny behandling`() {
        every { fagsakService.hentFagsak(fagsakId) } returns Fagsak(id = fagsakId,
                                                                    eksternId = EksternFagsakId(id = fagsakEksternId),
                                                                    stønadstype = Stønadstype.OVERGANGSSTØNAD)

        val slot = slot<OppdaterJournalpostRequest>()

        every { journalpostClient.oppdaterJournalpost(capture(slot), journalpostId) } returns OppdaterJournalpostResponse(
                journalpostId = journalpostId)

        every {
            journalpostClient.hentDokument(any(), any(), any())
        } returns "IKKE JSON".toByteArray()

        val behandleSakOppgaveId = journalføringService.fullførJournalpost(
                journalpostId = journalpostId,
                journalføringRequest = JournalføringRequest(
                        dokumentTitler,
                        fagsakId,
                        oppgaveId,
                        JournalføringBehandling(behandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING),
                        "Z1234567",
                        "1234"
                ),
        )

        assertThat(behandleSakOppgaveId).isEqualTo(nyOppgaveId)
        assertThat(slot.captured.sak?.fagsakId).isEqualTo(fagsakEksternId.toString())
        assertThat(slot.captured.sak?.sakstype).isEqualTo("FAGSAK")
        assertThat(slot.captured.sak?.fagsaksystem).isEqualTo("EF")
        dokumentTitler.forEach { (dokumentId, nyTittel) ->
            val oppdatertDokument = slot.captured.dokumenter?.find { dokument -> dokument.dokumentInfoId === dokumentId }
            assertThat(oppdatertDokument?.tittel).isEqualTo(nyTittel)
        }
    }
}