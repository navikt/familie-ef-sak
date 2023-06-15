package no.nav.familie.ef.sak.no.nav.familie.ef.sak.ekstern.journalføring

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.AVSLÅTT
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.HENLAGT
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.INNVILGET
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.OPPHØRT
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.REVURDERING
import no.nav.familie.ef.sak.ekstern.journalføring.AutomatiskJournalføringService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.journalføring.JournalføringService
import no.nav.familie.ef.sak.journalføring.JournalpostService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.BrukerIdType.AKTOERID
import no.nav.familie.kontrakter.felles.BrukerIdType.FNR
import no.nav.familie.kontrakter.felles.BrukerIdType.ORGNR
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AutomatiskJournalføringServiceTest {
    val journalføringService: JournalføringService = mockk()
    val behandlingService: BehandlingService = mockk()
    val fagsakService: FagsakService = mockk()
    val personService: PersonService = mockk()
    val infotrygdService: InfotrygdService = mockk()
    val arbeidsfordelingService: ArbeidsfordelingService = mockk()
    val journalpostService: JournalpostService = mockk()

    val automatiskJournalføringService = AutomatiskJournalføringService(
        journalføringService = journalføringService,
        behandlingService = behandlingService,
        fagsakService = fagsakService,
        personService = personService,
        infotrygdService = infotrygdService,
        arbeidsfordelingService = arbeidsfordelingService,
        journalpostService = journalpostService,
    )

    val enhet = "4489"
    val mappeId = null
    val personIdent = "123456789"
    val aktørId = "9876543210127"
    val tidligerePersonIdent = "9123456789"
    val personIdentAnnen = "9876543210"
    val aktørIdAnnen = "987654321012783123"
    val fagsak = fagsak()
    val journalpostId = "1"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        journalposttype = Journalposttype.I,
        journalstatus = Journalstatus.MOTTATT,
        dokumenter = emptyList(),
        bruker = Bruker(personIdent, FNR),
    )

    @BeforeEach
    internal fun setUp() {
        every { personService.hentPersonIdenter(any()) } returns PdlIdenter(
            identer = listOf(
                PdlIdent(personIdent, false),
                PdlIdent(tidligerePersonIdent, false),
            ),
        )
        every { fagsakService.hentEllerOpprettFagsak(any(), any()) } returns fagsak
        every { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) } returns enhet
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every {
            journalføringService.automatiskJournalfør(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns mockk()
    }

    @Test
    internal fun `kan ikke opprette førstegangsbehandling overgangsstønad hvis det eksisterer innslag i infotrygd`() {
        every { infotrygdService.eksisterer(any(), any()) } returns true
        every { fagsakService.finnFagsak(any(), any()) } returns null
        val kanOppretteBehandling =
            automatiskJournalføringService.kanOppretteBehandling(personIdent, StønadType.OVERGANGSSTØNAD)
        assertThat(kanOppretteBehandling).isFalse
    }

    @Test
    internal fun `kan opprette førstegangsbehandling barnetilsyn hvis det eksisterer innslag i infotrygd`() {
        every { infotrygdService.eksisterer(any(), any()) } returns true
        every { fagsakService.finnFagsak(any(), any()) } returns null
        val kanOppretteBehandling =
            automatiskJournalføringService.kanOppretteBehandling(personIdent, StønadType.BARNETILSYN)
        assertThat(kanOppretteBehandling).isTrue
    }

    @Test
    internal fun `kan ikke opprette behandling hvis det eksisterer en åpen behandling i ny løsning`() {
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling(status = BehandlingStatus.UTREDES))
        val kanOppretteBehandling =
            automatiskJournalføringService.kanOppretteBehandling(personIdent, StønadType.OVERGANGSSTØNAD)
        assertThat(kanOppretteBehandling).isFalse
    }

    @Test
    internal fun `kan opprette behandling hvis det ikke finnes innslag i infotrygd eller ny løsning`() {
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf()
        val kanOppretteBehandling =
            automatiskJournalføringService.kanOppretteBehandling(personIdent, StønadType.OVERGANGSSTØNAD)
        assertThat(kanOppretteBehandling).isTrue
    }

    @Test
    internal fun `kan opprette behandling hvis det ikke finnes innslag i infotrygd og alle behandlinger i ny løsning er henlagt`() {
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling(resultat = HENLAGT))
        val kanOppretteBehandling =
            automatiskJournalføringService.kanOppretteBehandling(personIdent, StønadType.OVERGANGSSTØNAD)
        assertThat(kanOppretteBehandling).isTrue
    }

    @Test
    internal fun `kan opprette behandling hvis det finnes innslag i infotrygd og alle behandlinger i ny løsning er ferdigstilt`() {
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(
            behandling(
                resultat = INNVILGET,
                status = BehandlingStatus.FERDIGSTILT,
            ),
            behandling(resultat = AVSLÅTT, status = BehandlingStatus.FERDIGSTILT),
        )
        val kanOppretteBehandling =
            automatiskJournalføringService.kanOppretteBehandling(personIdent, StønadType.OVERGANGSSTØNAD)
        assertThat(kanOppretteBehandling).isTrue
    }

    @Test
    internal fun `skal ikke kunne automatisk journalføre hvis journalpostens bruker og personident ikke samsvarer`() {
        val enAnnenBruker = Bruker(
            id = personIdentAnnen,
            type = FNR,
        )
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(bruker = enAnnenBruker)
        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
        val feil = assertThrows<Feil> {
            automatiskJournalføringService.automatiskJournalførTilBehandling(
                journalpostId,
                personIdent,
                StønadType.OVERGANGSSTØNAD,
                mappeId,
                OppgavePrioritet.NORM,
            )
        }
        assertThat(feil.message).contains("Ikke samsvar mellom personident på journalposten")
    }

    @Test
    internal fun `skal ikke kunne automatisk journalføre hvis journalpostens aktørId-bruker og personident ikke samsvarer`() {
        val enAnnenBruker = Bruker(
            id = aktørIdAnnen,
            type = AKTOERID,
        )
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(bruker = enAnnenBruker)
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every { personService.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent(personIdentAnnen, false)))
        val feil = assertThrows<Feil> {
            automatiskJournalføringService.automatiskJournalførTilBehandling(
                journalpostId,
                personIdent,
                StønadType.OVERGANGSSTØNAD,
                mappeId,
                OppgavePrioritet.NORM,
            )
        }
        assertThat(feil.message).contains("Ikke samsvar mellom personident på journalposten")
    }

    @Test
    internal fun `skal ikke kunne automatisk journalføre hvis journalpostens bruker mangler`() {
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(bruker = null)
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every { personService.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent(aktørId, false)))
        val feil = assertThrows<Feil> {
            automatiskJournalføringService.automatiskJournalførTilBehandling(
                journalpostId,
                personIdent,
                StønadType.OVERGANGSSTØNAD,
                mappeId,
                OppgavePrioritet.NORM,
            )
        }
        assertThat(feil.message).contains("Journalposten mangler bruker")
    }

    @Test
    internal fun `skal ikke kunne automatisk journalføre hvis journalpostens bruker er orgnr`() {
        val enAnnenBruker = Bruker(
            id = aktørIdAnnen,
            type = ORGNR,
        )
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(bruker = enAnnenBruker)
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every { personService.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent(aktørId, false)))
        val feil = assertThrows<Feil> {
            automatiskJournalføringService.automatiskJournalførTilBehandling(
                journalpostId,
                personIdent,
                StønadType.OVERGANGSSTØNAD,
                mappeId,
                OppgavePrioritet.NORM,
            )
        }
        assertThat(feil.message).contains("Ikke samsvar mellom personident på journalposten")
    }

    @Test
    internal fun `skal kunne automatisk journalføre hvis journalpostens aktørId-bruker og personident samsvarer`() {
        val aktørIdBruker = Bruker(
            id = aktørId,
            type = AKTOERID,
        )
        val journalpostMedAktørId = journalpost.copy(bruker = aktørIdBruker)
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpostMedAktørId
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak

        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every { personService.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent(aktørId, false)))
        automatiskJournalføringService.automatiskJournalførTilBehandling(
            journalpostId,
            personIdent,
            StønadType.OVERGANGSSTØNAD,
            mappeId,
            OppgavePrioritet.NORM,
        )
        verify {
            journalføringService.automatiskJournalfør(
                fagsak,
                journalpostMedAktørId,
                enhet,
                mappeId,
                FØRSTEGANGSBEHANDLING,
                OppgavePrioritet.NORM,
            )
        }
    }

    @Test
    internal fun `skal kunne automatisk journalføre hvis journalpostens personIdent er en historisk ident`() {
        val enAnnenBruker = Bruker(
            id = tidligerePersonIdent,
            type = FNR,
        )
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(bruker = enAnnenBruker)
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak

        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every {
            journalføringService.automatiskJournalfør(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns mockk()
        automatiskJournalføringService.automatiskJournalførTilBehandling(
            journalpostId,
            personIdent,
            StønadType.OVERGANGSSTØNAD,
            mappeId,
            OppgavePrioritet.NORM,
        )
    }

    @Test
    internal fun `skal ikke kunne automatisk journalføre hvis det finnes sak i infotrygd`() {
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost
        every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
        every { infotrygdService.eksisterer(any(), any()) } returns true
        val feil = assertThrows<Feil> {
            automatiskJournalføringService.automatiskJournalførTilBehandling(
                journalpostId,
                personIdent,
                StønadType.OVERGANGSSTØNAD,
                mappeId,
                OppgavePrioritet.NORM,
            )
        }
        assertThat(feil.message).contains("Kan ikke opprette førstegangsbehandling")
    }

    @Test
    internal fun `skal ikke kunne automatisk journalføre hvis det finnes ikke-henlagte behandlinger`() {
        every { infotrygdService.eksisterer(any(), any()) } returns false
        BehandlingResultat.values().filter { it != HENLAGT }.forEach { behandlingsresultat ->
            val behandling = behandling(fagsak = fagsak, resultat = behandlingsresultat, status = BehandlingStatus.FERDIGSTILT)
            val henlagtBehandling = behandling(fagsak = fagsak, resultat = HENLAGT, status = BehandlingStatus.FERDIGSTILT)
            every { journalpostService.hentJournalpost(journalpostId) } returns journalpost
            every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(
                henlagtBehandling,
                behandling,
                henlagtBehandling,
            )
            automatiskJournalføringService.automatiskJournalførTilBehandling(
                journalpostId,
                personIdent,
                StønadType.OVERGANGSSTØNAD,
                mappeId,
                OppgavePrioritet.NORM,
            )
        }
    }

    @Test
    internal fun `skal kunne automaitsk journalføre dersom det finnes behandlinger som er henlagte`() {
        val henlagtBehandling = behandling(fagsak = fagsak, resultat = HENLAGT, status = BehandlingStatus.FERDIGSTILT)
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak

        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(henlagtBehandling)
        automatiskJournalføringService.automatiskJournalførTilBehandling(
            journalpostId,
            personIdent,
            StønadType.OVERGANGSSTØNAD,
            mappeId,
            OppgavePrioritet.NORM,
        )
        verify {
            journalføringService.automatiskJournalfør(
                fagsak,
                journalpost,
                enhet,
                mappeId,
                FØRSTEGANGSBEHANDLING,
                OppgavePrioritet.NORM,
            )
        }
    }

    @Test
    internal fun `skal kunne automaitsk journalføre dersom det finnes behandlinger som er ferdigstilte`() {
        val førstegangsbehandling = behandling(fagsak = fagsak, resultat = INNVILGET, type = FØRSTEGANGSBEHANDLING, status = BehandlingStatus.FERDIGSTILT)
        val revurdering = behandling(fagsak = fagsak, resultat = OPPHØRT, type = REVURDERING, status = BehandlingStatus.FERDIGSTILT)
        val henlagtBehandling = behandling(fagsak = fagsak, resultat = HENLAGT, type = REVURDERING, status = BehandlingStatus.FERDIGSTILT)
        every { journalpostService.hentJournalpost(journalpostId) } returns journalpost
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak

        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(førstegangsbehandling, revurdering, henlagtBehandling)
        automatiskJournalføringService.automatiskJournalførTilBehandling(
            journalpostId,
            personIdent,
            StønadType.OVERGANGSSTØNAD,
            mappeId,
            OppgavePrioritet.NORM,
        )
        verify {
            journalføringService.automatiskJournalfør(
                fagsak,
                journalpost,
                enhet,
                mappeId,
                REVURDERING,
                OppgavePrioritet.NORM,
            )
        }
    }

    @Test
    internal fun `skal ikke kunne automatisk journalføre hvis journalposten har annen status enn MOTTATT`() {
        Journalstatus.values().filter { it != Journalstatus.MOTTATT }.forEach { journalstatus ->
            every { journalpostService.hentJournalpost(journalpostId) } returns journalpost.copy(journalstatus = journalstatus)
            every { behandlingService.hentBehandlinger(fagsak.id) } returns emptyList()
            val feil = assertThrows<Feil> {
                automatiskJournalføringService.automatiskJournalførTilBehandling(
                    journalpostId,
                    personIdent,
                    StønadType.OVERGANGSSTØNAD,
                    mappeId,
                    OppgavePrioritet.NORM,
                )
            }
            assertThat(feil.message).contains("Journalposten har ugyldig journalstatus $journalstatus")
        }
    }
}
