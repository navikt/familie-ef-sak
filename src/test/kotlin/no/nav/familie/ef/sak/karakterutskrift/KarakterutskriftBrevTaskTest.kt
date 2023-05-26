package no.nav.familie.ef.sak.karakterutskrift

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.brev.FrittståendeBrevService
import no.nav.familie.ef.sak.fagsak.FagsakService
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
}
