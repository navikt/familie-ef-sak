package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import com.fasterxml.jackson.module.kotlin.readValue
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
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.ForelderBarnRelasjon
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class BarnFyllerÅrOppfølgingsoppgaveServiceTest {

    private val gjeldendeBarnRepository = mockk<GjeldendeBarnRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val pdlClient = mockk<PdlClient>()
    private val oppgaveService = mockk<OppgaveService>()
    private val oppgaveRepository = mockk<OppgaveRepository>()
    private val taskRepository = mockk<TaskRepository>()
    private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    private val opprettOppgaveForBarnService =
        BarnFyllerÅrOppfølgingsoppgaveService(
            gjeldendeBarnRepository,
            oppgaveService,
            oppgaveRepository,
            personopplysningerIntegrasjonerClient,
            taskRepository,
            pdlClient
        )

    private val oppgaveSlot = slot<Oppgave>()
    private val oppgaveMock = mockk<Oppgave>()
    private val eksterneIderSlot = slot<Set<UUID>>()
    private val taskSlot = slot<Task>()

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
        every { taskRepository.save(capture(taskSlot)) } returns mockk()
        every { pdlClient.hentPersonForelderBarnRelasjon(any()) } returns emptyMap()
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
        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns setOf(
            BarnTilOppgave(
                barnTilUtplukkForOppgave.fødselsnummerBarn!!,
                barnTilUtplukkForOppgave.behandlingId,
                1,
                1
            )
        )
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
        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns setOf(
            BarnTilOppgave(
                barnTilUtplukkForOppgave.fødselsnummerBarn!!,
                barnTilUtplukkForOppgave.behandlingId,
                1,
                1
            )
        )

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
        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns setOf(
            BarnTilOppgave(
                barnTilUtplukkForOppgave.fødselsnummerBarn!!,
                barnTilUtplukkForOppgave.behandlingId,
                1,
                1
            )
        )
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

        val opprettBarnTilOppgave =
            opprettBarnForFødselsdatoer.map { BarnTilOppgave(it.fødselsnummerBarn!!, it.behandlingId, 1, 1) }.toSet()
        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns opprettBarnTilOppgave

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify(exactly = 5) { taskRepository.save(any()) }
    }

    @Test
    fun `barn med bare termindato fyller ett år samme dag, forvent kall til beskrivelseBarnFyllerEttÅr og ingen unntak`() {
        val termindato = LocalDate.now().minusYears(1)
        val fødselsnummerSøker = FnrGenerator.generer(1992)
        val fødselsnummerBarn = FnrGenerator.generer(termindato)

        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = null, termindato = termindato, fødselsnummerSøker = fødselsnummerSøker))

        every { pdlClient.hentPersonForelderBarnRelasjon(listOf(fødselsnummerSøker)) } returns mapOf(
            Pair(
                fødselsnummerSøker,
                PdlTestdataHelper.pdlBarn(
                    fødsel = PdlTestdataHelper.fødsel(fødselsdato = termindato),
                    forelderBarnRelasjon = listOf(
                        ForelderBarnRelasjon(
                            fødselsnummerBarn,
                            Familierelasjonsrolle.BARN,
                            Familierelasjonsrolle.MOR
                        )
                    )
                )
            )
        )

        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns setOf(
            BarnTilOppgave(
                fødselsnummerBarn,
                UUID.randomUUID(),
                1,
                1
            )
        )

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        val opprettOppgavePayload = objectMapper.readValue<OpprettOppgavePayload>(taskSlot.captured.payload)
        assertThat(opprettOppgavePayload.alder).isEqualTo(Alder.ETT_ÅR)
    }

    @Test
    fun `to barn som fyller år på samme behandling, forvent at bare en oppgave er gjeldende`() {
        val termindato = LocalDate.now().minusYears(1).minusDays(5)
        val behandlingId = UUID.randomUUID()
        val fødselsnummerSøker = FnrGenerator.generer(1992)
        val fødselsnummerBarn = FnrGenerator.generer(termindato)
        val fødselsnummerBarn2 = FnrGenerator.generer(termindato)

        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(
            opprettBarn(
                behandlingId = behandlingId,
                fødselsnummer = null,
                termindato = termindato,
                fødselsnummerSøker = fødselsnummerSøker
            ),
            opprettBarn(
                behandlingId = behandlingId,
                fødselsnummer = null,
                termindato = termindato,
                fødselsnummerSøker = fødselsnummerSøker
            )
        )

        every { pdlClient.hentPersonForelderBarnRelasjon(any()) } returns mapOf(
            Pair(
                fødselsnummerSøker,
                PdlTestdataHelper.pdlBarn(
                    fødsel = PdlTestdataHelper.fødsel(fødselsdato = termindato),
                    forelderBarnRelasjon = listOf(
                        ForelderBarnRelasjon(
                            fødselsnummerBarn,
                            Familierelasjonsrolle.BARN,
                            Familierelasjonsrolle.MOR
                        ),
                        ForelderBarnRelasjon(
                            fødselsnummerBarn2,
                            Familierelasjonsrolle.BARN,
                            Familierelasjonsrolle.MOR
                        )
                    )
                )
            )
        )

        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns setOf(
            BarnTilOppgave(
                fødselsnummerBarn,
                behandlingId,
                1,
                1
            )
        )

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()

        verify(exactly = 1) { taskRepository.save(any()) }
        val opprettOppgavePayload = objectMapper.readValue<OpprettOppgavePayload>(taskSlot.captured.payload)
        assertThat(opprettOppgavePayload.alder).isEqualTo(Alder.ETT_ÅR)
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

        val opprettBarnTilOppgave =
            opprettBarnForFødselsdato.map { BarnTilOppgave(it.fødselsnummerBarn!!, it.behandlingId, 1, 1) }.toSet()
        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns opprettBarnTilOppgave

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify(exactly = 2) { taskRepository.save(any()) }
    }

    @Test
    fun `barn fra vanlige behandlinger og migrerte fagsaker blir med i listen over oppgaver`() {
        val fødselsnummerBarn = FnrGenerator.generer(LocalDate.now().minusYears(1).minusDays(2))
        val fødselsdatoMigrert = LocalDate.now().minusDays(183)
        val fødselsnummerBarnMigrert = FnrGenerator.generer(fødselsdatoMigrert)
        val fødselsnummerSøker = FnrGenerator.generer(1992)
        val behandlingId = UUID.randomUUID()
        val migrertBehandlingId = UUID.randomUUID()

        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(behandlingId = behandlingId, fødselsnummer = fødselsnummerBarn, fødselsnummerSøker = fødselsnummerSøker))

        every {
            gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(
            opprettBarn(
                behandlingId = migrertBehandlingId,
                fødselsnummer = fødselsnummerBarnMigrert,
                fraMigrering = true
            )
        )

        every { gjeldendeBarnRepository.finnEksternFagsakIdForBehandlingId(any()) } returns setOf(
            BarnTilOppgave(
                fødselsnummerBarnMigrert,
                UUID.randomUUID(),
                1,
                1
            ),
            BarnTilOppgave(
                fødselsnummerBarn,
                UUID.randomUUID(),
                2,
                2
            )
        )

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify(exactly = 2) { taskRepository.save(any()) }
    }

    private fun opprettBarn(
        behandlingId: UUID = UUID.randomUUID(),
        fødselsnummerSøker: String = "12345678910",
        fødselsnummer: String? = null,
        termindato: LocalDate? = null,
        fraMigrering: Boolean = false
    ): BarnTilUtplukkForOppgave {
        return BarnTilUtplukkForOppgave(behandlingId, fødselsnummerSøker, fødselsnummer, termindato, fraMigrering)
    }
}
