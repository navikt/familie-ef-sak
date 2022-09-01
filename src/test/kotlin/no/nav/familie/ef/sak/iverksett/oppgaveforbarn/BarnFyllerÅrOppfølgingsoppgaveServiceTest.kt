package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.dto.EksternId
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.util.FnrGenerator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class BarnFyllerÅrOppfølgingsoppgaveServiceTest {

    private val gjeldendeBarnRepository = mockk<GjeldendeBarnRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val oppgaveService = mockk<OppgaveService>()
    private val oppgaveRepository = mockk<OppgaveRepository>()
    private val taskRepository = mockk<TaskRepository>()
    private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    private val opprettOppgaveForBarnService =
        BarnFyllerÅrOppfølgingsoppgaveService(gjeldendeBarnRepository, oppgaveClient, oppgaveService, oppgaveRepository, personopplysningerIntegrasjonerClient, taskRepository)

    private val oppgaveSlot = slot<Oppgave>()
    private val oppgaveMock = mockk<Oppgave>()
    private val eksterneIderSlot = slot<Set<UUID>>()

    @BeforeEach
    fun init() {
        oppgaveSlot.clear()
        eksterneIderSlot.clear()
        mockkObject(OppgaveBeskrivelse)
        every { gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(any(), any()) } returns emptyList()
        every { behandlingRepository.finnEksterneIder(capture(eksterneIderSlot)) } answers {
            firstArg<Set<UUID>>()
                .mapIndexed { index, behandlingId -> EksternId(behandlingId, index.toLong(), index.toLong()) }.toSet()
        }
        every { oppgaveRepository.findByBehandlingIdAndBarnPersonIdentAndAlder(any(), any(), any()) } returns null
        every { oppgaveService.lagFristForOppgave(any()) } returns LocalDate.now().plusDays(1)
        every { personopplysningerIntegrasjonerClient.hentNavEnhetForPersonMedRelasjoner(any()) } returns listOf(
            Arbeidsfordelingsenhet("enhetId", "enhetNavn")
        )
        every { oppgaveService.finnHendelseMappeId(any()) } returns 1
        every { oppgaveClient.opprettOppgave(any()) } returns 1
        every { oppgaveRepository.insert(capture(oppgaveSlot)) } returns oppgaveMock
        every { oppgaveRepository.findByTypeAndAlderIsNotNullAndBarnPersonIdenter(any(), any()) } returns emptyList()
        every { taskRepository.save(any()) } returns mockk()
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(OppgaveBeskrivelse)
    }

    @Test
    fun `barn har blitt mer enn 6 mnd, forvent kall til beskrivelseBarnBlirSeksMnd`() {
        val fødselsdato = LocalDate.now().minusDays(183)
        val barnTilUtplukkForOppgave = opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato))
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(barnTilUtplukkForOppgave)
        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns setOf(BarnTilOppgave(barnTilUtplukkForOppgave.fødselsnummerBarn!!, barnTilUtplukkForOppgave.behandlingId, 1, 1))
        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify { taskRepository.save(any()) }
    }

    @Test
    fun `barn har blitt mer enn ett år, forvent kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdato = LocalDate.now().minusYears(1).minusDays(1)
        val barnTilUtplukkForOppgave = opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato))
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(barnTilUtplukkForOppgave)
        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns setOf(BarnTilOppgave(barnTilUtplukkForOppgave.fødselsnummerBarn!!, barnTilUtplukkForOppgave.behandlingId, 1, 1))

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify { taskRepository.save(any()) }
    }

    @Test
    fun `barn blir seks mnd om en uke, forvent at det ikke blir opprettet oppgave`() {
        val fødselsdato = LocalDate.now().minusDays(182).plusWeeks(1)
        val barnTilUtplukkForOppgave = opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato))
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(barnTilUtplukkForOppgave)
        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns setOf(BarnTilOppgave(barnTilUtplukkForOppgave.fødselsnummerBarn!!, barnTilUtplukkForOppgave.behandlingId, 1, 1))
        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify(exactly = 0) { taskRepository.save(any()) }
    }

    @Test
    fun `5 av 13 barn har blitt 6 mnd, forvent at 7 oppgaver opprettes`() {
        val fødselsdatoer = (-5..7).asSequence().map { LocalDate.now().minusDays(182).plusDays(it.toLong()) }.toList()
        val opprettBarnForFødselsdatoer = fødselsdatoer.map { opprettBarn(fødselsnummer = FnrGenerator.generer(it)) }

        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns opprettBarnForFødselsdatoer

        val opprettBarnTilOppgave = opprettBarnForFødselsdatoer.map { BarnTilOppgave(it.fødselsnummerBarn!!, it.behandlingId, 1, 1) }.toSet()
        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns opprettBarnTilOppgave

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify(exactly = 5) { taskRepository.save(any()) }
    }

    @Test
    fun `to barn som fyller år på forskjellige behandlinger, forvent at to oppgaver er gjeldende`() {
        val fødselsdato = LocalDate.now().minusYears(1).minusDays(6)

        val opprettBarnForFødselsdato = listOf(
            opprettBarn(behandlingId = UUID.randomUUID(), fødselsnummer = FnrGenerator.generer(fødselsdato)),
            opprettBarn(behandlingId = UUID.randomUUID(), fødselsnummer = FnrGenerator.generer(fødselsdato))
        )
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns opprettBarnForFødselsdato

        val opprettBarnTilOppgave = opprettBarnForFødselsdato.map { BarnTilOppgave(it.fødselsnummerBarn!!, it.behandlingId, 1, 1) }.toSet()
        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns opprettBarnTilOppgave

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify(exactly = 2) { taskRepository.save(any()) }
    }

    @Test
    fun `barn fra vanlige behandlinger og migrerte fagsaker blir med i listen over oppgaver`() {
        val fødselsdato = LocalDate.now().minusDays(182).minusDays(4)

        val behandlingId = UUID.randomUUID()
        val migrertBehandlingId = UUID.randomUUID()
        val opprettetBarn = opprettBarn(behandlingId = behandlingId, FnrGenerator.generer(fødselsdato))
        val opprettBarnMigrering = opprettBarn(
            behandlingId = migrertBehandlingId,
            fødselsnummer = FnrGenerator.generer(fødselsdato),
            fraMigrering = true
        )
        val listOpprettedeBarn = listOf(opprettetBarn, opprettBarnMigrering)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettetBarn)

        every {
            gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarnMigrering)

        val opprettBarnTilOppgave = listOpprettedeBarn.map { BarnTilOppgave(it.fødselsnummerBarn!!, it.behandlingId, 1, 1) }.toSet()
        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns opprettBarnTilOppgave

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify(exactly = 2) { taskRepository.save(any()) }
    }

    private fun opprettBarn(
        behandlingId: UUID = UUID.randomUUID(),
        fødselsnummer: String? = null,
        fraMigrering: Boolean = false
    ): BarnTilUtplukkForOppgave {
        return BarnTilUtplukkForOppgave(behandlingId, "12345678910", fødselsnummer, fraMigrering)
    }
}
