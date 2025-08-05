package no.nav.familie.ef.sak.service

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.util.dagensDatoMedTidNorskFormat
import no.nav.familie.ef.sak.infrastruktur.config.OppgaveClientMock
import no.nav.familie.ef.sak.infrastruktur.exception.IntegrasjonException
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.AktivitetspliktigAlder
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveSubtype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.oppgave
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.BehandleSak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.BehandleUnderkjentVedtak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.GodkjenneVedtak
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.InnhentDokumentasjon
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype.VurderHenvendelse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum.FEILREGISTRERT
import no.nav.familie.kontrakter.felles.oppgave.StatusEnum.FERDIGSTILT
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.familie.ef.sak.oppgave.Oppgave as EfOppgave

internal class OppgaveServiceTest {
    private val oppgaveClient = OppgaveClientMock().oppgaveClient()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val fagsakService = mockk<FagsakService>()
    private val oppgaveRepository = mockk<OppgaveRepository>()
    private val cacheManager = ConcurrentMapCacheManager()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val personService = mockk<PersonService>()

    private val oppgaveService =
        OppgaveService(
            oppgaveClient,
            fagsakService,
            oppgaveRepository,
            arbeidsfordelingService,
            cacheManager,
            behandlingRepository,
            personService,
        )

    @BeforeEach
    internal fun setUp() {
        every { oppgaveClient.finnOppgaveMedId(any()) } returns lagEksternTestOppgave()
        every { oppgaveRepository.update(any()) } answers { firstArg() }
    }

    @Test
    fun `Skal ikke lage ny VurderHenvendelseOppgave med type utdanning dersom det finnes en på behandling fra før`() {
        val eksisterendeGsakOppgaveId: Long = 987345
        val oppgave =
            oppgave(
                behandling = behandling(),
                type = VurderHenvendelse,
                gsakOppgaveId = eksisterendeGsakOppgaveId,
            )
        mockFinnVurderHenvendelseOppgave(returnValue = oppgave)

        val opprettOppgave =
            oppgaveService.opprettOppgave(
                BEHANDLING_ID,
                VurderHenvendelse,
                OppgaveSubtype.INNSTILLING_VEDRØRENDE_UTDANNING,
            )

        verify(exactly = 0) { oppgaveRepository.insert(any()) }
        assertThat(opprettOppgave).isEqualTo(eksisterendeGsakOppgaveId)
    }

    @Test
    fun `Skal lage ny VurderHenvendelseOppgave med type utdanning dersom det ikke finnes en på behandling fra før`() {
        mockOpprettOppgave(slot())
        mockFinnVurderHenvendelseOppgave(null)
        val oppgaveRepositoryInsertSlot = slot<EfOppgave>()
        every { oppgaveRepository.insert(capture(oppgaveRepositoryInsertSlot)) } answers { firstArg() }

        oppgaveService.opprettOppgave(
            BEHANDLING_ID,
            VurderHenvendelse,
            OppgaveSubtype.INNSTILLING_VEDRØRENDE_UTDANNING,
        )

        assertThat(oppgaveRepositoryInsertSlot.captured.oppgaveSubtype).isEqualTo(OppgaveSubtype.INNSTILLING_VEDRØRENDE_UTDANNING)
    }

    private fun mockFinnVurderHenvendelseOppgave(returnValue: Oppgave?) {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndOppgaveSubtype(
                any(),
                any(),
                any(),
            )
        } returns returnValue
    }

    @Test
    fun `Opprett oppgave skal samle data og opprette en ny oppgave basert på fagsak, behandling, fnr og enhet`() {
        val slot = slot<OpprettOppgaveRequest>()
        mockOpprettOppgave(slot)

        oppgaveService.opprettOppgave(BEHANDLING_ID, BehandleSak)

        assertThat(slot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_EKSTERN_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(OppgaveIdentV2(ident = FNR, gruppe = IdentGruppe.FOLKEREGISTERIDENT))
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.Overgangsstønad.value)
        assertThat(slot.captured.fristFerdigstillelse).isAfterOrEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.ENF)
        val forventetBeskrivelse = "--- ${dagensDatoMedTidNorskFormat()} (familie-ef-sak) --- \nOppgave opprettet"
        assertThat(slot.captured.beskrivelse).isEqualTo(forventetBeskrivelse)
    }

    @Test
    fun `Opprett oppgave som feiler med fordeling skal prøve på nytt med 4489`() {
        val slot = slot<OpprettOppgaveRequest>()
        mockOpprettOppgave(slot)
        every { arbeidsfordelingService.hentNavEnhetId(any(), any()) } returns null
        oppgaveService.opprettOppgave(BEHANDLING_ID, BehandleSak)

        verify(exactly = 2) { oppgaveClient.opprettOppgave(any()) }
    }

    @Test
    fun `Opprett oppgave som feiler på en ukjent måte skal bare kaste feil videre`() {
        val slot = slot<OpprettOppgaveRequest>()
        mockOpprettOppgave(slot)
        every { oppgaveClient.opprettOppgave(any()) } throws IntegrasjonException("En merkelig feil vi ikke kjenner til")
        assertThrows<IntegrasjonException> {
            oppgaveService.opprettOppgave(BEHANDLING_ID, BehandleSak)
        }
    }

    @Test
    fun `Skal legge i mappe når vi oppretter godkjenne vedtak-oppgave for 4489`() {
        val slot = slot<OpprettOppgaveRequest>()
        mockOpprettOppgave(slot)

        val beskrivelse = "Oppgave tekst her"
        oppgaveService.opprettOppgave(behandlingId = BEHANDLING_ID, oppgavetype = GodkjenneVedtak, beskrivelse = beskrivelse)

        assertThat(slot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(slot.captured.mappeId).isNotNull
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_EKSTERN_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(OppgaveIdentV2(ident = FNR, gruppe = IdentGruppe.FOLKEREGISTERIDENT))
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.Overgangsstønad.value)
        assertThat(slot.captured.fristFerdigstillelse).isAfterOrEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.ENF)
        val forventetBeskrivelse = "--- ${dagensDatoMedTidNorskFormat()} (familie-ef-sak) --- \n$beskrivelse"
        assertThat(slot.captured.beskrivelse).isEqualTo(forventetBeskrivelse)
    }

    @Test
    fun `Skal ikke legge oppgave i mappe når det er godkjenne vedtak-oppgave for enhet ulik 4489`() {
        every { fagsakService.hentFagsakForBehandling(BEHANDLING_ID) } returns lagTestFagsak()
        every { oppgaveRepository.insert(any()) } returns lagTestOppgave()
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns null
        every { arbeidsfordelingService.hentNavEnhetId(any(), any()) } returns "1234"
        val slot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(slot)) } returns GSAK_OPPGAVE_ID

        oppgaveService.opprettOppgave(BEHANDLING_ID, GodkjenneVedtak, beskrivelse = "")

        assertThat(slot.captured.enhetsnummer).isEqualTo("1234")
        assertThat(slot.captured.mappeId).isNull()
        assertThat(slot.captured.saksId).isEqualTo(FAGSAK_EKSTERN_ID.toString())
        assertThat(slot.captured.ident).isEqualTo(OppgaveIdentV2(ident = FNR, gruppe = IdentGruppe.FOLKEREGISTERIDENT))
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.Overgangsstønad.value)
        assertThat(slot.captured.fristFerdigstillelse).isAfterOrEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.ENF)
        val forventetBeskrivelse = "--- ${dagensDatoMedTidNorskFormat()} (familie-ef-sak) --- \n"

        assertThat(slot.captured.beskrivelse).isEqualTo(forventetBeskrivelse)
    }

    @Test
    fun `Skal kunne hente oppgave gitt en ID`() {
        every { oppgaveClient.finnOppgaveMedId(any()) } returns lagEksternTestOppgave()
        val oppgave = oppgaveService.hentOppgave(GSAK_OPPGAVE_ID)

        assertThat(oppgave.id).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Finn oppgave sist endret i familie-ef-sak`() {
        val oppgaveList = oppgaveList() // Ny, eldre og eldst

        every {
            oppgaveRepository.findByBehandlingIdAndTypeIn(
                BEHANDLING_ID,
                setOf(BehandleSak, GodkjenneVedtak, BehandleUnderkjentVedtak),
            )
        } returns oppgaveList

        every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID) } returns lagEksternTestOppgave().copy(status = FEILREGISTRERT)
        every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID_2) } returns lagEksternTestOppgave().copy(status = FERDIGSTILT)
        every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID_3) } returns lagEksternTestOppgave().copy(status = FERDIGSTILT)

        val oppgave = oppgaveService.finnBehandlingsoppgaveSistEndretIEFSak(BEHANDLING_ID)

        // skal finne nyeste oppgave som er feilregistrert
        assertThat(oppgave!!.status).isEqualTo(FEILREGISTRERT)

        verify(exactly = 0) { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID_3) }
        verify(exactly = 0) { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID_2) }
        verify(exactly = 1) { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID) }
    }

    private fun oppgaveList(): List<Oppgave> {
        val testOppgave1 = lagTestOppgave(GSAK_OPPGAVE_ID)
        val testOppgave2 = lagTestOppgave(GSAK_OPPGAVE_ID_2)
        val testOppgave3 = lagTestOppgave(GSAK_OPPGAVE_ID_3)

        val sporbarNyest = testOppgave1.sporbar.copy(endret = Endret(endretAv = "123", endretTid = LocalDateTime.now().minusDays(1)))
        val sporbarMellom = testOppgave2.sporbar.copy(endret = Endret(endretAv = "123", endretTid = LocalDateTime.now().minusDays(2)))
        val sporbarGammel = testOppgave3.sporbar.copy(endret = Endret(endretAv = "123", endretTid = LocalDateTime.now().minusDays(3)))

        val ny = testOppgave1.copy(sporbar = sporbarNyest)
        val mellom = testOppgave2.copy(sporbar = sporbarMellom)
        val gammel = testOppgave3.copy(sporbar = sporbarGammel)
        return listOf(mellom, ny, gammel)
    }

    @Test
    fun `Skal hente oppgaver gitt en filtrering`() {
        every { oppgaveClient.hentOppgaver(any()) } returns lagFinnOppgaveResponseDto()
        val respons = oppgaveService.hentOppgaver(FinnOppgaveRequest(tema = Tema.ENF))

        assertThat(respons.antallTreffTotalt).isEqualTo(1)
        assertThat(respons.oppgaver.first().id).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Ferdigstill oppgave`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns lagTestOppgave()
        every { oppgaveRepository.update(any()) } returns lagTestOppgave()
        val slot = slot<Long>()
        every { oppgaveClient.ferdigstillOppgave(capture(slot)) } just runs

        oppgaveService.ferdigstillBehandleOppgave(BEHANDLING_ID, BehandleSak)
        assertThat(slot.captured).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Ferdigstill oppgave feiler fordi den ikke finner oppgave på behandlingen`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns null
        every { oppgaveRepository.insert(any()) } returns lagTestOppgave()

        assertThatThrownBy {
            oppgaveService.ferdigstillBehandleOppgave(
                BEHANDLING_ID,
                BehandleSak,
            )
        }.hasMessage("Finner ikke oppgave for behandling $BEHANDLING_ID")
            .isInstanceOf(java.lang.IllegalStateException::class.java)
    }

    @Test
    fun `Ferdigstill oppgave hvis oppgave ikke finnes - kaster ikke feil`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns null
        oppgaveService.ferdigstillOppgaveForBehandlingIdOgOppgavetype(BEHANDLING_ID, BehandleSak)
    }

    @Test
    fun `Ferdigstill oppgave - hvis oppgave finnes`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns lagTestOppgave()
        every { oppgaveRepository.update(any()) } returns lagTestOppgave()
        val slot = slot<Long>()
        every { oppgaveClient.ferdigstillOppgave(capture(slot)) } just runs

        oppgaveService.ferdigstillOppgaveForBehandlingIdOgOppgavetype(BEHANDLING_ID, BehandleSak)
        assertThat(slot.captured).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Fordel oppgave skal tildele oppgave til saksbehandler`() {
        val oppgaveSlot = slot<Long>()
        val saksbehandlerSlot = slot<String>()

        every { oppgaveClient.fordelOppgave(capture(oppgaveSlot), capture(saksbehandlerSlot)) } returns GSAK_OPPGAVE_ID

        val id = oppgaveService.fordelOppgave(GSAK_OPPGAVE_ID, SAKSBEHANDLER_ID)

        assertThat(GSAK_OPPGAVE_ID).isEqualTo(oppgaveSlot.captured)
        assertThat(SAKSBEHANDLER_ID).isEqualTo(saksbehandlerSlot.captured)
        assertThat(id).isEqualTo(GSAK_OPPGAVE_ID)
        verify(exactly = 1) { oppgaveClient.fordelOppgave(any(), any(), any()) }
    }

    @Test
    fun `Fordel oppgave skal ikke tildele oppgave til saksbehandler dersom saksbehandler allerde er tildelt oppgaven`() {
        every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID) } returns lagEksternTestOppgave(SAKSBEHANDLER_ID)

        val id = oppgaveService.fordelOppgave(GSAK_OPPGAVE_ID, SAKSBEHANDLER_ID)

        assertThat(id).isEqualTo(GSAK_OPPGAVE_ID)
        verify(exactly = 0) { oppgaveClient.fordelOppgave(any(), any(), any()) }
    }

    @Test
    fun `Tilbakestill oppgave skal nullstille tildeling på oppgave`() {
        val oppgaveSlot = slot<Long>()
        every { oppgaveClient.fordelOppgave(capture(oppgaveSlot), any()) } returns GSAK_OPPGAVE_ID

        oppgaveService.tilbakestillFordelingPåOppgave(GSAK_OPPGAVE_ID)

        assertThat(GSAK_OPPGAVE_ID).isEqualTo(oppgaveSlot.captured)
        verify(exactly = 1) { oppgaveClient.fordelOppgave(any(), null) }
    }

    @Test
    fun `Skal sette frist for oppgave`() {
        val frister =
            listOf<Pair<LocalDateTime, LocalDate>>(
                Pair(torsdag.morgen(), fredagFrist),
                Pair(torsdag.kveld(), mandagFrist),
                Pair(fredag.morgen(), mandagFrist),
                Pair(fredag.kveld(), tirsdagFrist),
                Pair(lørdag.morgen(), tirsdagFrist),
                Pair(lørdag.kveld(), tirsdagFrist),
                Pair(søndag.morgen(), tirsdagFrist),
                Pair(søndag.kveld(), tirsdagFrist),
                Pair(mandag.morgen(), tirsdagFrist),
                Pair(mandag.kveld(), onsdagFrist),
            )

        frister.forEach {
            assertThat(oppgaveService.lagFristForOppgave(it.first)).isEqualTo(it.second)
        }
    }

    @Test
    internal fun `finnMapper - skal cache mapper`() {
        oppgaveService.finnMapper("4489")
        oppgaveService.finnMapper("4489")

        verify(exactly = 1) { oppgaveClient.finnMapper(any(), any()) }
        oppgaveService.finnMapper("4483")
        verify(exactly = 2) { oppgaveClient.finnMapper(any(), any()) }
    }

    @Test
    internal fun `skal cache mapper når man kaller på den indirekte`() {
        val slot = slot<OpprettOppgaveRequest>()
        mockOpprettOppgave(slot)

        oppgaveService.opprettOppgave(BEHANDLING_ID, GodkjenneVedtak)
        oppgaveService.opprettOppgave(BEHANDLING_ID, GodkjenneVedtak)

        verify(exactly = 1) { oppgaveClient.finnMapper(any(), any()) }
    }

    @Test
    fun `sjekk at oppgaver av type InnhentDokumentasjon blir lagt i hendelse-mappe`() {
        val behandlingId = UUID.randomUUID()
        every { oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any()) } returns null
        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsak()
        every { arbeidsfordelingService.hentNavEnhetId(any(), any()) } returns "4489"
        every { oppgaveRepository.insert(any()) } answers { firstArg() }
        val opprettOppgaveRequestSlot = slot<OpprettOppgaveRequest>()
        every { oppgaveClient.opprettOppgave(capture(opprettOppgaveRequestSlot)) } returns 1

        oppgaveService.opprettOppgave(
            behandlingId,
            InnhentDokumentasjon,
            null,
            AktivitetspliktigAlder.ETT_ÅR.oppgavebeskrivelse,
        )

        assertThat(opprettOppgaveRequestSlot.captured.mappeId).isEqualTo(105)
    }

    @Nested
    inner class FeilregistertOppgave {
        private val feilregistrertException =
            RessursException(
                Ressurs.failure("Oppgave har status feilregistrert"),
                HttpServerErrorException(HttpStatus.BAD_REQUEST),
            )

        private val annenException =
            RessursException(
                Ressurs.failure("Oppgave har status ferdigstilt"),
                HttpServerErrorException(HttpStatus.BAD_REQUEST),
            )

        @BeforeEach
        internal fun setUp() {
            every {
                oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
            } returns lagTestOppgave()
        }

        @Test
        internal fun `skal ignorere feil fra ferdigstillOppgave hvis den er feilregistrert og vi skal ignorere feilregistrert`() {
            every { oppgaveClient.ferdigstillOppgave(any()) } throws feilregistrertException

            oppgaveService.ferdigstillOppgaveForBehandlingIdOgOppgavetype(UUID.randomUUID(), BehandleSak, true)

            verify(exactly = 1) { oppgaveRepository.update(any()) }
        }

        @Test
        internal fun `skal kaste feil hvis oppgaven er feilregistrert og vi ikke ignorerer feilregistrerte`() {
            every { oppgaveClient.ferdigstillOppgave(any()) } throws feilregistrertException

            assertThatThrownBy {
                oppgaveService.ferdigstillOppgaveForBehandlingIdOgOppgavetype(UUID.randomUUID(), BehandleSak, false)
            }.isInstanceOf(RessursException::class.java)

            verify(exactly = 0) { oppgaveRepository.update(any()) }
        }

        @Test
        internal fun `skal kaste feil hvis oppgaven allerede er ferdigstilt og feilmeldingen er ferdigstilt`() {
            every { oppgaveClient.ferdigstillOppgave(any()) } throws annenException

            assertThatThrownBy {
                oppgaveService.ferdigstillOppgaveForBehandlingIdOgOppgavetype(UUID.randomUUID(), BehandleSak, true)
            }.isInstanceOf(RessursException::class.java)

            verify(exactly = 0) { oppgaveRepository.update(any()) }
        }
    }

    @Nested
    inner class HenleggBehandlingUtenÅFerdigstilleOppgave {
        @Test
        internal fun `Dersom oppgave ikke finnes skal det ikke kastes feil`() {
            every {
                oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
            } returns null
            val ferdigstiltOppgave = oppgaveService.settEfOppgaveTilFerdig(BEHANDLING_ID, BehandleSak)

            verify(exactly = 1) { oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any()) }
            verify(exactly = 0) { oppgaveRepository.update(any()) }
            assertThat(ferdigstiltOppgave).isNull()
        }

        @Test
        fun `Ferdigstill oppgave - hvis oppgave finnes`() {
            every {
                oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
            } returns lagTestOppgave()
            every { oppgaveRepository.update(any()) } returns lagTestOppgave().copy(erFerdigstilt = true)

            val ferdigstiltOppgave = oppgaveService.settEfOppgaveTilFerdig(BEHANDLING_ID, BehandleSak)
            assertThat(ferdigstiltOppgave).isNotNull
            assertThat(ferdigstiltOppgave?.behandlingId).isEqualTo(BEHANDLING_ID)
            assertThat(ferdigstiltOppgave?.erFerdigstilt).isTrue()
        }
    }

    private fun mockOpprettOppgave(slot: CapturingSlot<OpprettOppgaveRequest>) {
        every { fagsakService.hentFagsakForBehandling(BEHANDLING_ID) } returns lagTestFagsak()

        every { oppgaveRepository.insert(any()) } returns lagTestOppgave()

        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns null
        every { arbeidsfordelingService.hentNavEnhetId(any(), any()) } returns ENHETSNUMMER
        every { oppgaveClient.opprettOppgave(capture(slot)) } answers {
            val oppgaveRequest: OpprettOppgaveRequest = firstArg()
            if (oppgaveRequest.enhetsnummer == null) {
                throw IntegrasjonException("Fant ingen gyldig arbeidsfordeling for oppgaven")
            } else {
                GSAK_OPPGAVE_ID
            }
        }
    }

    private fun lagTestFagsak(): Fagsak =
        fagsak(
            id = FAGSAK_ID,
            stønadstype = StønadType.OVERGANGSSTØNAD,
            eksternId = FAGSAK_EKSTERN_ID,
            identer = setOf(PersonIdent(ident = FNR)),
        )

    private fun lagTestOppgave(gsakOppgaveId: Long = GSAK_OPPGAVE_ID): Oppgave = Oppgave(behandlingId = BEHANDLING_ID, type = BehandleSak, gsakOppgaveId = gsakOppgaveId)

    private fun lagEksternTestOppgave(tilordnetRessurs: String? = null): no.nav.familie.kontrakter.felles.oppgave.Oppgave =
        no.nav.familie.kontrakter.felles.oppgave
            .Oppgave(id = GSAK_OPPGAVE_ID, tilordnetRessurs = tilordnetRessurs)

    private fun lagFinnOppgaveResponseDto(): FinnOppgaveResponseDto =
        FinnOppgaveResponseDto(
            antallTreffTotalt = 1,
            oppgaver = listOf(lagEksternTestOppgave()),
        )

    companion object {
        private val FAGSAK_ID = UUID.fromString("1242f220-cad3-4640-95c1-190ec814c91e")
        private const val FAGSAK_EKSTERN_ID = 98765L
        private const val GSAK_OPPGAVE_ID = 12345L
        private const val GSAK_OPPGAVE_ID_2 = 54321L
        private const val GSAK_OPPGAVE_ID_3 = 98765L
        private val BEHANDLING_ID = UUID.fromString("1c4209bd-3217-4130-8316-8658fe300a84")
        private const val ENHETSNUMMER = "4489"
        private const val FNR = "11223312345"
        private const val SAKSBEHANDLER_ID = "Z999999"
    }
}

private fun LocalDateTime.kveld(): LocalDateTime = this.withHour(20)

private fun LocalDateTime.morgen(): LocalDateTime = this.withHour(8)

private val torsdag = LocalDateTime.of(2021, 4, 1, 12, 0)
private val fredag = LocalDateTime.of(2021, 4, 2, 12, 0)
private val lørdag = LocalDateTime.of(2021, 4, 3, 12, 0)
private val søndag = LocalDateTime.of(2021, 4, 4, 12, 0)
private val mandag = LocalDateTime.of(2021, 4, 5, 12, 0)

private val fredagFrist = LocalDate.of(2021, 4, 2)
private val mandagFrist = LocalDate.of(2021, 4, 5)
private val tirsdagFrist = LocalDate.of(2021, 4, 6)
private val onsdagFrist = LocalDate.of(2021, 4, 7)
