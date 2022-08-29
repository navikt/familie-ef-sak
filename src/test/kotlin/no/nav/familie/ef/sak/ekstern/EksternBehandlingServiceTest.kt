package no.nav.familie.ef.sak.ekstern

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat.HENLAGT
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EksternBehandlingServiceTest {
    val tilkjentYtelseService: TilkjentYtelseService = mockk()
    val behandlingService: BehandlingService = mockk()
    val fagsakService: FagsakService = mockk()
    val personService: PersonService = mockk()
    val infotrygdService: InfotrygdService = mockk()

    val eksternBehandlingService = EksternBehandlingService(
        tilkjentYtelseService = tilkjentYtelseService,
        behandlingService = behandlingService,
        fagsakService = fagsakService,
        personService = personService,
        infotrygdService = infotrygdService
    )

    val personIdent = "123456789"
    val fagsak = fagsak()

    @BeforeEach
    internal fun setUp() {
        every { personService.hentPersonIdenter(any()) } returns PdlIdenter(identer = listOf(PdlIdent(personIdent, false)))
    }

    @Test
    internal fun `kan ikke opprette førstegangsbehandling hvis det eksisterer innslag i infotrygd`() {
        every { infotrygdService.eksisterer(any(), any()) } returns true
        every { fagsakService.finnFagsak(any(), any()) } returns null
        val kanOppretteFørstegangsbehandling =
            eksternBehandlingService.kanOppretteFørstegangsbehandling(personIdent, StønadType.OVERGANGSSTØNAD)
        Assertions.assertThat(kanOppretteFørstegangsbehandling).isFalse
    }

    @Test
    internal fun `kan ikke opprette førstegangsbehandling hvis det eksisterer innslag i ny løsning`() {
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling())
        val kanOppretteFørstegangsbehandling =
            eksternBehandlingService.kanOppretteFørstegangsbehandling(personIdent, StønadType.OVERGANGSSTØNAD)
        Assertions.assertThat(kanOppretteFørstegangsbehandling).isFalse
    }

    @Test
    internal fun `kan opprette førstegangsbehandling hvis det ikke finnes innslag i infotrygd eller ny løsning`() {
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf()
        val kanOppretteFørstegangsbehandling =
            eksternBehandlingService.kanOppretteFørstegangsbehandling(personIdent, StønadType.OVERGANGSSTØNAD)
        Assertions.assertThat(kanOppretteFørstegangsbehandling).isTrue
    }

    @Test
    internal fun `kan opprette førstegangsbehandling hvis det ikke finnes innslag i infotrygd og ingen fagsak i ny løsning`() {
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every { fagsakService.finnFagsak(any(), any()) } returns null
        val kanOppretteFørstegangsbehandling =
            eksternBehandlingService.kanOppretteFørstegangsbehandling(personIdent, StønadType.OVERGANGSSTØNAD)
        Assertions.assertThat(kanOppretteFørstegangsbehandling).isTrue
    }

    @Test
    internal fun `kan opprette førstegangsbehandling hvis det ikke finnes innslag i infotrygd og alle behandlinger i ny løsning er henlagt`() {
        every { infotrygdService.eksisterer(any(), any()) } returns false
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling(resultat = HENLAGT))
        val kanOppretteFørstegangsbehandling =
            eksternBehandlingService.kanOppretteFørstegangsbehandling(personIdent, StønadType.OVERGANGSSTØNAD)
        Assertions.assertThat(kanOppretteFørstegangsbehandling).isTrue
    }
}
