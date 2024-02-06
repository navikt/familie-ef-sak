package no.nav.familie.ef.sak.no.nav.familie.ef.sak.forvaltning.karakterutskrift

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.brev.FrittståendeBrevService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.forvaltning.karakterutskrift.SendKarakterutskriftBrevTilIverksettTask
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Year

internal class KarakterutskriftBrevTaskTest {

    private val behandlingService = mockk<BehandlingService>()
    private val fagsakService = mockk<FagsakService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val frittståendeBrevService = mockk<FrittståendeBrevService>()
    private val iverksettClient = mockk<IverksettClient>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()

    private val karakterutskriftBrevTask = SendKarakterutskriftBrevTilIverksettTask(
        behandlingService,
        fagsakService,
        oppgaveService,
        frittståendeBrevService,
        personopplysningerService,
        iverksettClient,
        arbeidsfordelingService,
    )

    @BeforeEach
    fun setUp() {
        every { personopplysningerService.hentPersonopplysningerUtenVedtakshistorikk(any<String>()) } returns mockk {
            every { vergemål } returns emptyList()
        }
    }

    @Test
    internal fun `task skal feile dersom det ikke finnes fagsak for ident på oppgaven`() {
        val oppgaveId: Long = 123

        every { oppgaveService.hentOppgave(oppgaveId) } returns Oppgave(
            id = oppgaveId,
            identer = listOf(OppgaveIdentV2("11111111", IdentGruppe.FOLKEREGISTERIDENT)),
        )
        every { fagsakService.finnFagsaker(any()) } returns emptyList()

        val task = SendKarakterutskriftBrevTilIverksettTask.opprettTask(oppgaveId, FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE, Year.of(2023))

        val feil = assertThrows<Feil> { karakterutskriftBrevTask.doTask(task) }
        assertThat(feil.frontendFeilmelding?.contains("Fant ikke fagsak"))
    }

    @Test
    internal fun `task skal feile dersom det ikke finnes behandling for ident på oppgaven`() {
        val oppgaveId: Long = 123

        every { oppgaveService.hentOppgave(oppgaveId) } returns Oppgave(
            id = oppgaveId,
            identer = listOf(OppgaveIdentV2("11111111", IdentGruppe.FOLKEREGISTERIDENT)),
        )
        every { behandlingService.finnesBehandlingForFagsak(any()) } returns false
        every { fagsakService.finnFagsaker(any()) } returns listOf(fagsak())

        val task = SendKarakterutskriftBrevTilIverksettTask.opprettTask(oppgaveId, FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE, Year.of(2023))

        val feil = assertThrows<Feil> { karakterutskriftBrevTask.doTask(task) }
        assertThat(feil.frontendFeilmelding?.contains("Fant ikke behandling"))
    }

    @Test
    internal fun `task skal feile dersom bruker har vergemål`() {
        val oppgaveId: Long = 123

        every { oppgaveService.hentOppgave(oppgaveId) } returns Oppgave(
            id = oppgaveId,
            identer = listOf(OppgaveIdentV2("11111111", IdentGruppe.FOLKEREGISTERIDENT)),
        )
        every { behandlingService.finnesBehandlingForFagsak(any()) } returns true
        every { fagsakService.finnFagsaker(any()) } returns listOf(fagsak())
        every { personopplysningerService.hentPersonopplysningerUtenVedtakshistorikk(any<String>()) } returns mockk {
            every { vergemål } returns listOf(mockk())
        }

        val task = SendKarakterutskriftBrevTilIverksettTask.opprettTask(oppgaveId, FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE, Year.of(2023))

        val feil = assertThrows<Feil> { karakterutskriftBrevTask.doTask(task) }
        assertThat(feil.frontendFeilmelding?.contains("Kan ikke automatisk sende brev for oppgaveId=$oppgaveId. Brev om innhenting av karakterutskrift skal ikke sendes automatisk fordi bruker har vergemål. Saken må følges opp manuelt og tasken kan avvikshåndteres."))
    }
}
