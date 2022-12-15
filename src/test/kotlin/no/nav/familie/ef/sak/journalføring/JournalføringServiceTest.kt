package no.nav.familie.ef.sak.journalføring

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.behandling.migrering.InfotrygdPeriodeValideringService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.EksternFagsakId
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.felles.util.mockFeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.familie.ef.sak.journalføring.dto.BarnSomSkalFødes
import no.nav.familie.ef.sak.journalføring.dto.JournalføringBehandling
import no.nav.familie.ef.sak.journalføring.dto.JournalføringRequest
import no.nav.familie.ef.sak.journalføring.dto.JournalføringTilNyBehandlingRequest
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil.mockVilkårGrunnlagDto
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak.SØKNAD
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus.BAD_REQUEST
import java.time.LocalDate
import java.util.UUID

internal class JournalføringServiceTest {

    private val journalpostClient = mockk<JournalpostClient>()
    private val behandlingService = mockk<BehandlingService>()
    private val søknadService = mockk<SøknadService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val fagsakService = mockk<FagsakService>()
    private val vurderingService = mockk<VurderingService>()
    private val taskService = mockk<TaskService>()
    private val barnService = mockk<BarnService>()
    private val iverksettService = mockk<IverksettService>(relaxed = true)
    private val featureToggleService = mockFeatureToggleService()
    private val journalpostService = JournalpostService(journalpostClient = journalpostClient)
    private val infotrygdPeriodeValideringService = mockk<InfotrygdPeriodeValideringService>()

    private val journalføringService =
        JournalføringService(
            behandlingService = behandlingService,
            søknadService = søknadService,
            fagsakService = fagsakService,
            vurderingService = vurderingService,
            grunnlagsdataService = mockk(relaxed = true),
            iverksettService = iverksettService,
            oppgaveService = oppgaveService,
            taskService = taskService,
            barnService = barnService,
            featureToggleService = featureToggleService,
            journalpostService = journalpostService,
            infotrygdPeriodeValideringService = infotrygdPeriodeValideringService
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
    private val journalpost = Journalpost(
        avsenderMottaker = JournalføringTestUtil.avsenderMottaker,
        journalpostId = journalpostId,
        journalposttype = Journalposttype.I,
        journalstatus = Journalstatus.MOTTATT,
        tema = "ENF",
        behandlingstema = "ab0071",
        dokumenter =
        listOf(
            DokumentInfo(
                dokumentInfoIdMedJsonVerdi,
                "Vedlegg1",
                brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                dokumentvarianter =
                listOf(
                    Dokumentvariant(Dokumentvariantformat.ORIGINAL),
                    Dokumentvariant(Dokumentvariantformat.ARKIV)
                )
            ),
            DokumentInfo(
                "99999",
                "Vedlegg2",
                brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                dokumentvarianter =
                listOf(Dokumentvariant(Dokumentvariantformat.ARKIV))
            ),
            DokumentInfo(
                "23456",
                "Vedlegg3",
                brevkode = "XYZ"
            ),
            DokumentInfo(
                "88888",
                "Vedlegg4",
                brevkode = "XYZ"
            )
        ),
        tittel = "Søknad om overgangsstønad"
    )

    private val ustrukturertJournalpost = journalpost.copy(dokumenter = emptyList())

    private val slotJournalpost = slot<OppdaterJournalpostRequest>()
    private val slotOpprettedeTasks = mutableListOf<Task>()

    @BeforeEach
    fun setupMocks() {
        every { journalpostClient.hentJournalpost(journalpostId) } returns (journalpost)

        every { fagsakService.hentEksternId(any()) } returns fagsakEksternId
        every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak

        justRun {
            barnService.opprettBarnPåBehandlingMedSøknadsdata(any(), any(), any(), any(), any(), any(), any())
        }

        every { behandlingService.finnesBehandlingForFagsak(any()) } returns true

        every {
            fagsakService.hentFagsak(any())
        } returns fagsak(
            identer = fagsakpersoner(setOf("1")),
            id = fagsakId,
            eksternId = EksternFagsakId(fagsakEksternId)
        )

        mockOpprettBehandling(behandlingId)

        every { oppgaveService.ferdigstillOppgave(any()) } just runs
        every { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) } returns nyOppgaveId
        every { behandlingService.leggTilBehandlingsjournalpost(any(), any(), any()) } just runs
        every { journalpostClient.ferdigstillJournalpost(any(), any(), any()) } just runs

        every {
            søknadService.lagreSøknadForOvergangsstønad(any(), any(), any(), any())
        } just Runs

        every { taskService.save(capture(slotOpprettedeTasks)) } answers { firstArg() }

        slotJournalpost.clear()
        every {
            journalpostClient.oppdaterJournalpost(capture(slotJournalpost), journalpostId, any())
        } returns OppdaterJournalpostResponse(journalpostId = journalpostId)

        BrukerContextUtil.mockBrukerContext("saksbehandlernavn")
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
        clearMocks(vurderingService)
    }

    @Test
    internal fun `skal fullføre manuell journalføring på eksisterende behandling`() {
        val journalførtOppgaveId =
            journalføringService.fullførJournalpost(
                journalpostId = journalpostId,
                journalføringRequest = lagRequest(JournalføringBehandling(behandlingId))
            )

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
        assertThat(slotOpprettedeTasks).isEmpty()
        verify(exactly = 0) { søknadService.lagreSøknadForOvergangsstønad(any(), any(), any(), any()) }
        verify(exactly = 0) { iverksettService.startBehandling(any(), any()) }
        verify(exactly = 1) { journalpostClient.ferdigstillJournalpost(any(), any(), any()) }
        verify(exactly = 1) { oppgaveService.ferdigstillOppgave(oppgaveId.toLong()) }
    }

    @Test
    internal fun `skal fullføre manuell journalføring på ny behandling`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(
            id = fagsakId,
            eksternId = EksternFagsakId(id = fagsakEksternId),
            stønadstype = StønadType.OVERGANGSSTØNAD
        )

        every {
            journalpostClient.hentOvergangsstønadSøknad(any(), any())
        } returns Testsøknad.søknadOvergangsstønad
        every { infotrygdPeriodeValideringService.validerKanOppretteBehandlingGittInfotrygdData(any()) } just Runs

        val behandleSakOppgaveId =
            journalføringService.fullførJournalpost(
                journalpostId = journalpostId,
                journalføringRequest = lagRequest(JournalføringBehandling(behandlingstype = FØRSTEGANGSBEHANDLING))
            )

        assertThat(behandleSakOppgaveId).isEqualTo(nyOppgaveId)
        assertThat(slotJournalpost.captured.sak?.fagsakId).isEqualTo(fagsakEksternId.toString())
        assertThat(slotJournalpost.captured.sak?.sakstype).isEqualTo("FAGSAK")
        assertThat(slotJournalpost.captured.sak?.fagsaksystem).isEqualTo(Fagsystem.EF)
        dokumentTitler.forEach { (dokumentId, nyTittel) ->
            val oppdatertDokument =
                slotJournalpost.captured.dokumenter?.find { dokument -> dokument.dokumentInfoId === dokumentId }
            assertThat(oppdatertDokument?.tittel).isEqualTo(nyTittel)
        }
        assertThat(slotOpprettedeTasks.map { it.type }).doesNotContain(OpprettOppgaveForOpprettetBehandlingTask.TYPE)
    }

    @Test
    internal fun `manuell journalføring på ny behandling kaster feil hvis personen finnes i infotrygd`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(
            id = fagsakId,
            eksternId = EksternFagsakId(id = fagsakEksternId),
            stønadstype = StønadType.OVERGANGSSTØNAD
        )

        every {
            journalpostClient.hentOvergangsstønadSøknad(any(), any())
        } returns Testsøknad.søknadOvergangsstønad

        mockFeilerValideringAvInfotrygdperioder()

        assertThatThrownBy {
            journalføringService.fullførJournalpost(
                journalpostId = journalpostId,
                journalføringRequest = lagRequest(JournalføringBehandling(behandlingstype = FØRSTEGANGSBEHANDLING))
            )
        }.isInstanceOf(ApiFeil::class.java)
    }

    @Test
    internal fun `skal opprette behandling og knytte til søknad for ferdigstilt journalpost`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(
            id = fagsakId,
            eksternId = EksternFagsakId(id = fagsakEksternId),
            stønadstype = StønadType.OVERGANGSSTØNAD
        )
        every { journalpostClient.hentJournalpost(journalpostId) } returns (journalpost.copy(journalstatus = Journalstatus.JOURNALFOERT))
        every {
            journalpostClient.hentOvergangsstønadSøknad(any(), any())
        } returns Testsøknad.søknadOvergangsstønad
        every { infotrygdPeriodeValideringService.validerKanOppretteBehandlingGittInfotrygdData(any()) } just Runs

        val behandleSakOppgaveId =
            journalføringService.opprettBehandlingMedSøknadsdataFraEnFerdigstiltJournalpost(
                journalpostId = journalpostId,
                journalføringRequest = JournalføringTilNyBehandlingRequest(
                    fagsakId = fagsakId,
                    behandlingstype = FØRSTEGANGSBEHANDLING
                )
            )

        assertThat(behandleSakOppgaveId).isEqualTo(nyOppgaveId)
    }

    @Test
    internal fun `skal feile med opprettelse av behandling for ferdigstilt journalpost dersom journalposten ikke er ferdigstilt`() {
        every { fagsakService.hentFagsak(fagsakId) } returns fagsak(
            id = fagsakId,
            eksternId = EksternFagsakId(id = fagsakEksternId),
            stønadstype = StønadType.OVERGANGSSTØNAD
        )
        every { journalpostClient.hentJournalpost(journalpostId) } returns (journalpost.copy(journalstatus = Journalstatus.MOTTATT))

        assertThrows<ApiFeil> {
            journalføringService.opprettBehandlingMedSøknadsdataFraEnFerdigstiltJournalpost(
                journalpostId = journalpostId,
                journalføringRequest = JournalføringTilNyBehandlingRequest(
                    fagsakId = fagsakId,
                    behandlingstype = FØRSTEGANGSBEHANDLING
                )
            )
        }
    }

    @Test
    internal fun `opprettBehandlingMedSøknadsdataFraEnFerdigstiltJournalpost skal kaste feil hvis finnes i infotrygd`() {
        every { journalpostClient.hentJournalpost(journalpostId) } returns (journalpost.copy(journalstatus = Journalstatus.JOURNALFOERT))
        every {
            journalpostClient.hentOvergangsstønadSøknad(any(), any())
        } returns Testsøknad.søknadOvergangsstønad

        mockFeilerValideringAvInfotrygdperioder()

        assertThatThrownBy {
            journalføringService.opprettBehandlingMedSøknadsdataFraEnFerdigstiltJournalpost(
                journalpostId = journalpostId,
                journalføringRequest = JournalføringTilNyBehandlingRequest(
                    fagsakId = fagsakId,
                    behandlingstype = FØRSTEGANGSBEHANDLING
                )
            )
        }.isInstanceOf(ApiFeil::class.java)
    }

    @Nested
    inner class validerJournalføringNyBehandling {

        @Test
        internal fun `strukturert søknad - kan ikke sende inn dokumentasjonstype`() {
            assertThatThrownBy {
                fullførJournalpost(
                    JournalføringBehandling(
                        ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.ETTERSENDING,
                        behandlingstype = BehandlingType.REVURDERING
                    ),
                    VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE
                )
            }.hasMessage("Kan ikke sende inn dokumentasjonstype når journalposten har strukturert søknad")
        }

        @Test
        internal fun `ustrukturert søknad - må sende inn dokumentasjonstype`() {
            every { journalpostClient.hentJournalpost(journalpostId) } returns (ustrukturertJournalpost)

            assertThatThrownBy {
                fullførJournalpost(
                    JournalføringBehandling(
                        ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.IKKE_VALGT,
                        behandlingstype = BehandlingType.REVURDERING
                    ),
                    VilkårsbehandleNyeBarn.IKKE_VALGT
                )
            }.hasMessage("Må sende inn dokumentasjonstype når journalposten mangler digital søknad")
        }
    }

    @Nested
    inner class Papirsøknad {

        @BeforeEach
        internal fun setUp() {
            every { journalpostClient.hentJournalpost(journalpostId) } returns (ustrukturertJournalpost)
        }

        @Test
        internal fun `ny behandling - skal ikke kopiere vurderinger fra forrige behandling`() {
            val forrigeBehandlingId = UUID.randomUUID()
            mockOpprettBehandling(behandlingId, forrigeBehandlingId)
            every { infotrygdPeriodeValideringService.validerKanOppretteBehandlingGittInfotrygdData(any()) } just Runs

            fullførJournalpost(
                JournalføringBehandling(
                    ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.PAPIRSØKNAD,
                    behandlingstype = BehandlingType.REVURDERING
                ),
                VilkårsbehandleNyeBarn.IKKE_VALGT
            )
            verify(exactly = 0) { vurderingService.kopierVurderingerTilNyBehandling(any(), any(), any(), any()) }
        }
    }

    @Nested
    inner class Ettersending {

        @BeforeEach
        internal fun setUp() {
            every { journalpostClient.hentJournalpost(journalpostId) } returns (ustrukturertJournalpost)
        }

        @Test
        internal fun `kan velge å ta med eller ikke ta med barn på ny behandling`() {
            every { infotrygdPeriodeValideringService.validerKanOppretteBehandlingGittInfotrygdData(any()) } just Runs
            listOf(VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE, VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE).forEach {
                fullførJournalpost(
                    JournalføringBehandling(
                        ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.ETTERSENDING,
                        behandlingstype = BehandlingType.REVURDERING
                    ),
                    it
                )
            }
        }

        @Test
        internal fun `eksisterende behandling behandling - skal ikke kopiere vurderinger fra forrige behandling`() {
            mockOpprettBehandling(behandlingId)
            fullførJournalpost(
                JournalføringBehandling(
                    ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.ETTERSENDING,
                    behandlingstype = BehandlingType.REVURDERING,
                    behandlingsId = behandlingId
                ),
                VilkårsbehandleNyeBarn.IKKE_VALGT
            )
            verify(exactly = 0) { vurderingService.kopierVurderingerTilNyBehandling(any(), any(), any(), any()) }
        }

        @Test
        internal fun `ny behandling - skal kopiere vurderinger fra forrige behandling etter at barn er opprettet`() {
            val forrigeBehandlingId = UUID.randomUUID()
            mockOpprettBehandling(behandlingId, forrigeBehandlingId)
            justRun {
                vurderingService.kopierVurderingerTilNyBehandling(forrigeBehandlingId, behandlingId, any(), any())
            }
            every { infotrygdPeriodeValideringService.validerKanOppretteBehandlingGittInfotrygdData(any()) } just Runs
            every { vurderingService.hentGrunnlagOgMetadata(behandlingId) } returns
                Pair(
                    mockVilkårGrunnlagDto(),
                    HovedregelMetadata(null, Sivilstandstype.UGIFT, false, emptyList(), emptyList())
                )
            fullførJournalpost(
                JournalføringBehandling(
                    ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.ETTERSENDING,
                    behandlingstype = BehandlingType.REVURDERING
                ),
                VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE
            )
            verifyOrder {
                barnService.opprettBarnPåBehandlingMedSøknadsdata(any(), any(), any(), any(), any(), any(), any())
                vurderingService.kopierVurderingerTilNyBehandling(forrigeBehandlingId, behandlingId, any(), any())
            }
        }

        @Test
        internal fun `kan ikke journalføres på ny førstegangsbehandling`() {
            assertThatThrownBy {
                fullførJournalpost(
                    JournalføringBehandling(
                        ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.ETTERSENDING,
                        behandlingstype = FØRSTEGANGSBEHANDLING
                    ),
                    VilkårsbehandleNyeBarn.IKKE_VALGT
                )
            }.hasMessage("Må journalføre ettersending på ny behandling som revurdering")
        }

        @Test
        internal fun `ny behandling må ha valgt vilkårsbehandle nye barn`() {
            assertThatThrownBy {
                fullførJournalpost(
                    JournalføringBehandling(
                        ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.ETTERSENDING,
                        behandlingstype = BehandlingType.REVURDERING
                    ),
                    VilkårsbehandleNyeBarn.IKKE_VALGT
                )
            }.hasMessage("Man må velge om man skal vilkårsbehandle nye barn på ny behandling av type ettersending")
        }

        @Test
        internal fun `kan ikke sende inn barn som skal fødes`() {
            assertThatThrownBy {
                fullførJournalpost(
                    JournalføringBehandling(
                        ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.ETTERSENDING,
                        behandlingstype = BehandlingType.REVURDERING
                    ),
                    VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE,
                    listOf(BarnSomSkalFødes(LocalDate.now()))
                )
            }.hasMessage("Årsak må være satt til papirsøknad hvis man sender inn barn som skal fødes")
        }

        @Test
        internal fun `kan ikke sende inn vilkårsbehandleNyeBarn på eksisterende behandling`() {
            listOf(VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE, VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE).forEach {
                assertThatThrownBy {
                    fullførJournalpost(
                        JournalføringBehandling(
                            ustrukturertDokumentasjonType = UstrukturertDokumentasjonType.ETTERSENDING,
                            behandlingsId = UUID.randomUUID()
                        ),
                        it
                    )
                }.hasMessage("Kan ikke vilkårsbehandle nye barn på en eksisterende behandling")
            }
        }
    }

    @Nested
    inner class AutomatiskJournalføring {

        @BeforeEach
        internal fun setUp() {
            every { journalpostClient.hentJournalpost(journalpostId) } returns (ustrukturertJournalpost)
        }

        @Test
        internal fun `skal automatisk journalføre en ny digital søknad`() {
            every {
                journalpostClient.hentOvergangsstønadSøknad(any(), any())
            } returns Testsøknad.søknadOvergangsstønad

            val journalførendeEnhet = "4489"
            val mappeId = 1234L
            val res = journalføringService.automatiskJournalførFørstegangsbehandling(fagsak, journalpost, journalførendeEnhet, mappeId)

            verify { journalpostClient.oppdaterJournalpost(any(), journalpostId, null) }
            verify { journalpostClient.ferdigstillJournalpost(journalpostId, journalførendeEnhet, null) }
            verify { iverksettService.startBehandling(any(), fagsak) }
            verify { søknadService.lagreSøknadForOvergangsstønad(any(), any(), any(), any()) }
            verify {
                behandlingService.opprettBehandling(
                    behandlingType = FØRSTEGANGSBEHANDLING,
                    fagsakId = fagsakId,
                    behandlingsårsak = SØKNAD
                )
            }
            verify(exactly = 0) { oppgaveService.opprettOppgave(any(), any(), any(), any(), any()) }
            verify { barnService.opprettBarnPåBehandlingMedSøknadsdata(any(), any(), any(), any(), any(), any(), any()) }
            assertThat(res.behandlingId).isEqualTo(behandlingId)
            assertThat(res.fagsakId).isEqualTo(fagsakId)
            assertThat(slotOpprettedeTasks.map { it.type }).containsExactly(OpprettOppgaveForOpprettetBehandlingTask.TYPE)
        }
    }

    private fun mockOpprettBehandling(behandlingId: UUID, forrigeBehandlingId: UUID? = null) {
        val behandling = Behandling(
            id = behandlingId,
            fagsakId = fagsakId,
            type = FØRSTEGANGSBEHANDLING,
            status = BehandlingStatus.UTREDES,
            steg = StegType.VILKÅR,
            resultat = BehandlingResultat.IKKE_SATT,
            årsak = SØKNAD,
            forrigeBehandlingId = forrigeBehandlingId
        )
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandlingService.opprettBehandling(any(), any(), behandlingsårsak = any()) } returns behandling
    }

    private fun fullførJournalpost(
        journalføringBehandling: JournalføringBehandling,
        vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn,
        barnSomSkalFødes: List<BarnSomSkalFødes> = emptyList()
    ) {
        journalføringService.fullførJournalpost(
            lagRequest(journalføringBehandling, vilkårsbehandleNyeBarn, barnSomSkalFødes),
            journalpostId
        )
    }

    private fun lagRequest(
        journalføringBehandling: JournalføringBehandling,
        vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.IKKE_VALGT,
        barnSomSkalFødes: List<BarnSomSkalFødes> = emptyList()
    ): JournalføringRequest {
        return JournalføringRequest(
            dokumentTitler,
            fagsakId,
            oppgaveId,
            journalføringBehandling,
            "1234",
            barnSomSkalFødes,
            vilkårsbehandleNyeBarn
        )
    }

    private fun mockFeilerValideringAvInfotrygdperioder() {
        every {
            infotrygdPeriodeValideringService.validerKanOppretteBehandlingGittInfotrygdData(any())
        } throws ApiFeil("feil", BAD_REQUEST)
    }

    // Test barnetilsyn!!!
}
