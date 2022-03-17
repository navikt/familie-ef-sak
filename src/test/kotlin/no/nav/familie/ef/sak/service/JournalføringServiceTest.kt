package no.nav.familie.ef.sak.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.EksternFagsakId
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.familie.ef.sak.journalføring.JournalføringService
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.journalføring.dto.JournalføringBehandling
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequest
import no.nav.familie.ef.sak.journalføring.dto.JournalføringTilNyBehandlingRequest
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class JournalføringServiceTest {

    private val journalpostClient = mockk<JournalpostClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val søknadService = mockk<SøknadService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val fagsakService = mockk<FagsakService>()
    private val pdlClient = mockk<PdlClient>()
    private val taskRepository = mockk<TaskRepository>()
    private val barnService = mockk<BarnService>()
    private val iverksettService = mockk<IverksettService>(relaxed = true)
    private val featureToggleService = mockk<FeatureToggleService>(relaxed = true)

    private val journalføringService =
            JournalføringService(
                    journalpostClient = journalpostClient,
                    behandlingService = behandlingService,
                    søknadService = søknadService,
                    fagsakService = fagsakService,
                    pdlClient = pdlClient,
                    grunnlagsdataService = mockk(relaxed = true),
                    iverksettService = iverksettService,
                    oppgaveService = oppgaveService,
                    taskRepository = taskRepository,
                    barnService = barnService,
                    featureToggleService = featureToggleService
            )

    private val fagsakEksternId = 12345L
    private val fagsak = fagsak(eksternId = EksternFagsakId(fagsakEksternId))
    private val fagsakId: UUID = fagsak.id
    private val journalpostId = "98765"
    private val nyOppgaveId = 999999L
    private val behandlingId: UUID = UUID.randomUUID()
    private val oppgaveId = "1234567"
    private val dokumentTitler = hashMapOf("12345" to "Asbjørns skilsmissepapirer", "23456" to "Eiriks samværsdokument")
    private val dokumentInfoIdMedJsonVerdi = "12345"
    private val journalpost = Journalpost(journalpostId = journalpostId,
                                          journalposttype = Journalposttype.I,
                                          journalstatus = Journalstatus.MOTTATT,
                                          tema = "ENF",
                                          behandlingstema = "ab0071",
                                          dokumenter =
                                          listOf(DokumentInfo(dokumentInfoIdMedJsonVerdi,
                                                              "Vedlegg1",
                                                              brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                                              dokumentvarianter =
                                                              listOf(Dokumentvariant(Dokumentvariantformat.ORIGINAL),
                                                                     Dokumentvariant(Dokumentvariantformat.ARKIV))),
                                                 DokumentInfo("99999",
                                                              "Vedlegg2",
                                                              brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                                              dokumentvarianter =
                                                              listOf(Dokumentvariant(Dokumentvariantformat.ARKIV))),
                                                 DokumentInfo("23456",
                                                              "Vedlegg3",
                                                              brevkode = "XYZ"),
                                                 DokumentInfo("88888",
                                                              "Vedlegg4",
                                                              brevkode = "XYZ")),
                                          tittel = "Søknad om overgangsstønad")

    @BeforeEach
    fun setupMocks() {
        every { journalpostClient.hentJournalpost(journalpostId) } returns (journalpost)

        every { fagsakService.hentEksternId(any()) } returns fagsakEksternId
        every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak

        every { barnService.opprettBarnPåBehandlingMedSøknadsdata(any(), any(), any(), any()) } just Runs

        every { behandlingService.hentBehandling(behandlingId) }
                .returns(Behandling(id = behandlingId,
                                    fagsakId = fagsakId,
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    status = BehandlingStatus.UTREDES,
                                    steg = StegType.VILKÅR,
                                    resultat = BehandlingResultat.IKKE_SATT,
                                    årsak = BehandlingÅrsak.SØKNAD))

        every { behandlingService.opprettBehandling(any(), any(), behandlingsårsak = any()) }
                .returns(Behandling(id = behandlingId,
                                    fagsakId = fagsakId,
                                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                    status = BehandlingStatus.UTREDES,
                                    steg = StegType.VILKÅR,
                                    resultat = BehandlingResultat.IKKE_SATT,
                                    årsak = BehandlingÅrsak.SØKNAD))

        every { oppgaveService.ferdigstillOppgave(any()) } just runs
        every { oppgaveService.opprettOppgave(any(), any(), any(), any()) } returns nyOppgaveId
        every { behandlingService.leggTilBehandlingsjournalpost(any(), any(), any()) } just runs
        every { journalpostClient.ferdigstillJournalpost(any(), any(), any()) } just runs

        every {
            søknadService.lagreSøknadForOvergangsstønad(any(), any(), any(), any())
        } just Runs

        every { taskRepository.save(any()) } answers { firstArg() }

        BrukerContextUtil.mockBrukerContext("saksbehandlernavn")
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal fullføre manuell journalføring på eksisterende behandling`() {
        val slotJournalpost = slot<OppdaterJournalpostRequest>()

        every {
            journalpostClient.oppdaterJournalpost(capture(slotJournalpost),
                                                  journalpostId,
                                                  any())
        } returns OppdaterJournalpostResponse(journalpostId = journalpostId)

        every {
            fagsakService.hentFagsak(any())
        } returns fagsak().copy(id = fagsakId, eksternId = EksternFagsakId(fagsakEksternId))

        val journalførtOppgaveId =
                journalføringService.fullførJournalpost(journalpostId = journalpostId,
                                                        journalføringRequest =
                                                        JournalføringRequest(dokumentTitler,
                                                                             fagsakId,
                                                                             oppgaveId,
                                                                             JournalføringBehandling(behandlingId),
                                                                             "1234"))

        assertThat(journalførtOppgaveId).isEqualTo(oppgaveId.toLong())
        assertThat(slotJournalpost.captured.sak?.fagsakId).isEqualTo(fagsakEksternId.toString())
        assertThat(slotJournalpost.captured.sak?.sakstype).isEqualTo("FAGSAK")
        assertThat(slotJournalpost.captured.sak?.fagsaksystem).isEqualTo(Fagsystem.EF)
        dokumentTitler.forEach { (dokumentId, nyTittel) ->
            val oppdatertDokument =
                    slotJournalpost.captured.dokumenter?.find { dokument -> dokument.dokumentInfoId === dokumentId }
            assertThat(oppdatertDokument?.tittel).isEqualTo(nyTittel)
        }
        assertThat(slotJournalpost.captured.dokumenter).hasSize(4)
        verify(exactly = 0) { søknadService.lagreSøknadForOvergangsstønad(any(), any(), any(), any()) }
        verify(exactly = 0) { iverksettService.startBehandling(any(), any()) }
        verify(exactly = 1) { journalpostClient.ferdigstillJournalpost(any(), any(), any()) }
        verify(exactly = 1) { oppgaveService.ferdigstillOppgave(oppgaveId.toLong()) }
    }

    @Test
    internal fun `skal fullføre manuell journalføring på ny behandling`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(id = fagsakId,
                                                                    eksternId = EksternFagsakId(id = fagsakEksternId),
                                                                    stønadstype = Stønadstype.OVERGANGSSTØNAD)

        val slot = slot<OppdaterJournalpostRequest>()

        every { journalpostClient.oppdaterJournalpost(capture(slot), journalpostId, any()) }
                .returns(OppdaterJournalpostResponse(journalpostId = journalpostId))

        every {
            journalpostClient.hentOvergangsstønadSøknad(any(), any())
        } returns Testsøknad.søknadOvergangsstønad

        val behandleSakOppgaveId =
                journalføringService.fullførJournalpost(
                        journalpostId = journalpostId,
                        journalføringRequest =
                        JournalføringRequest(dokumentTitler,
                                             fagsakId,
                                             oppgaveId,
                                             JournalføringBehandling(behandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING),
                                             "1234"))

        assertThat(behandleSakOppgaveId).isEqualTo(nyOppgaveId)
        assertThat(slot.captured.sak?.fagsakId).isEqualTo(fagsakEksternId.toString())
        assertThat(slot.captured.sak?.sakstype).isEqualTo("FAGSAK")
        assertThat(slot.captured.sak?.fagsaksystem).isEqualTo(Fagsystem.EF)
        dokumentTitler.forEach { (dokumentId, nyTittel) ->
            val oppdatertDokument = slot.captured.dokumenter?.find { dokument -> dokument.dokumentInfoId === dokumentId }
            assertThat(oppdatertDokument?.tittel).isEqualTo(nyTittel)
        }
    }


    @Test
    internal fun `skal opprette behandling og knytte til søknad for ferdigstilt journalpost`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(id = fagsakId,
                                                                    eksternId = EksternFagsakId(id = fagsakEksternId),
                                                                    stønadstype = Stønadstype.OVERGANGSSTØNAD)
        every { journalpostClient.hentJournalpost(journalpostId) } returns (journalpost.copy(journalstatus = Journalstatus.JOURNALFOERT))
        every {
            journalpostClient.hentOvergangsstønadSøknad(any(), any())
        } returns Testsøknad.søknadOvergangsstønad

        val behandleSakOppgaveId =
                journalføringService.opprettBehandlingMedSøknadsdataFraEnFerdigstiltJournalpost(
                        journalpostId = journalpostId,
                        journalføringRequest = JournalføringTilNyBehandlingRequest(fagsakId = fagsakId,
                                                                                   behandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING))

        assertThat(behandleSakOppgaveId).isEqualTo(nyOppgaveId)
    }


    @Test
    internal fun `skal feile med opprettelse av behandling for ferdigstilt journalpost dersom journalposten ikke er ferdigstilt`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(id = fagsakId,
                                                                    eksternId = EksternFagsakId(id = fagsakEksternId),
                                                                    stønadstype = Stønadstype.OVERGANGSSTØNAD)
        every { journalpostClient.hentJournalpost(journalpostId) } returns (journalpost.copy(journalstatus = Journalstatus.MOTTATT))

        assertThrows<ApiFeil> {
            journalføringService.opprettBehandlingMedSøknadsdataFraEnFerdigstiltJournalpost(
                    journalpostId = journalpostId,
                    journalføringRequest = JournalføringTilNyBehandlingRequest(fagsakId = fagsakId,
                                                                               behandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING))
        }
    }

    // Test barnetilsyn!!!
}
