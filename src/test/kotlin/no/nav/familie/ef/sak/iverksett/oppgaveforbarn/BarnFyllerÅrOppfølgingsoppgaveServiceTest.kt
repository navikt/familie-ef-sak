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
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.ForelderBarnRelasjon
import no.nav.familie.ef.sak.repository.barnMedIdent
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.oppgave
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødsel
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
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
    private val personService = mockk<PersonService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val oppgaveRepository = mockk<OppgaveRepository>()
    private val taskService = mockk<TaskService>()
    private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    private val grunnlagsdataService = mockk<GrunnlagsdataService>()

    private val opprettOppgaveForBarnService =
        BarnFyllerÅrOppfølgingsoppgaveService(
            gjeldendeBarnRepository,
            oppgaveRepository,
            taskService,
            personService,
            grunnlagsdataService,
        )

    private val oppgaveSlot = slot<Oppgave>()
    private val oppgaveMock = mockk<Oppgave>()
    private val eksterneIderSlot = slot<Set<UUID>>()
    private val taskSlot = slot<Task>()

    private val grunnlagsDataMedMetadata = mockk<GrunnlagsdataMedMetadata>()
    private val grunnlagsdataDomene = mockk<GrunnlagsdataDomene>()

    @BeforeEach
    fun init() {
        oppgaveSlot.clear()
        eksterneIderSlot.clear()
        mockkObject(OppgaveBeskrivelse)
        every { gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(any(), any()) } returns emptyList()
        every { behandlingRepository.finnEksterneIder(capture(eksterneIderSlot)) } answers {
            firstArg<Set<UUID>>()
                .mapIndexed { index, behandlingId -> EksternId(behandlingId, index.toLong(), index.toLong()) }
                .toSet()
        }
        every { oppgaveRepository.findByBehandlingIdAndBarnPersonIdentAndAlder(any(), any(), any()) } returns null
        every { oppgaveService.lagFristForOppgave(any()) } returns LocalDate.now().plusDays(1)
        every { personopplysningerIntegrasjonerClient.hentNavEnhetForPersonMedRelasjoner(any()) } returns
            listOf(
                Arbeidsfordelingsenhet("enhetId", "enhetNavn"),
            )
        every { oppgaveService.finnHendelseMappeId(any()) } returns 1
        every { oppgaveClient.opprettOppgave(any()) } returns 1
        every { oppgaveRepository.insert(capture(oppgaveSlot)) } returns oppgaveMock
        every { oppgaveRepository.findByTypeAndAlderIsNotNullAndBarnPersonIdenter(any(), any()) } returns emptyList()
        every { taskService.save(capture(taskSlot)) } returns mockk()
        every { personService.hentPersonForelderBarnRelasjon(any()) } returns emptyMap()
        every { grunnlagsDataMedMetadata.grunnlagsdata } returns grunnlagsdataDomene
        every { grunnlagsdataDomene.barn } returns listOf(barnMedIdent("04042495250", "fornavn etternavn"))
        every { grunnlagsdataService.hentGrunnlagsdataForBehandlinger(any()) } answers { firstArg<Set<UUID>>().associateWith { grunnlagsDataMedMetadata } }
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(OppgaveBeskrivelse)
    }

    @Test
    fun `barn har blitt mer enn 6 mnd, forvent kall til beskrivelseBarnBlirSeksMnd`() {
        val fødselsdato = LocalDate.now().minusMonths(6)
        val fødselsnummer = FnrGenerator.generer()
        val barnTilUtplukkForOppgave = opprettBarn(fødselsnummer = fødselsnummer)
        every { grunnlagsdataDomene.barn } returns listOf(barnMedIdent(fødselsnummer, "Fornavn etternavn", fødsel(fødselsdato)))
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(barnTilUtplukkForOppgave)
        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify { taskService.save(any()) }
    }

    @Test
    fun `barn har blitt mer enn ett år, forvent kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdato = LocalDate.now().minusYears(1).minusDays(1)
        val fødselsnummer = FnrGenerator.generer()
        val barnTilUtplukkForOppgave = opprettBarn(fødselsnummer = fødselsnummer)
        every { grunnlagsdataDomene.barn } returns listOf(barnMedIdent(fødselsnummer, "Fornavn etternavn", fødsel(fødselsdato)))
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(barnTilUtplukkForOppgave)

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify { taskService.save(any()) }
    }

    @Test
    fun `barn blir seks mnd om en uke, forvent at det ikke blir opprettet oppgave`() {
        val fødselsdato = LocalDate.now().minusDays(182).plusWeeks(1)
        val fødselsnummer = FnrGenerator.generer()
        val barnTilUtplukkForOppgave = opprettBarn(fødselsnummer = fødselsnummer)
        every { grunnlagsdataDomene.barn } returns listOf(barnMedIdent(fødselsnummer, "Fornavn etternavn", fødsel(fødselsdato)))
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(barnTilUtplukkForOppgave)

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `7 barn er innenfor cutoff etter at de ble 6 mnd, forvent 7 oppgaver opprettes`() {
        val fødselsdatoer = (-20..20).asSequence().map { LocalDate.now().minusMonths(6).plusDays(it.toLong()) }.toList()
        val opprettBarnForFødselsdatoer = fødselsdatoer.map { opprettBarn(fødselsnummer = FnrGenerator.generer(it)) }
        every { grunnlagsdataDomene.barn } returns opprettBarnForFødselsdatoer.mapIndexed { i, it -> barnMedIdent(it.fødselsnummerBarn.toString(), "fornavn etternavn", fødsel(fødselsdatoer[i])) }
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns opprettBarnForFødselsdatoer

        val opprettBarnTilOppgave =
            opprettBarnForFødselsdatoer.map { BarnTilOppgave(it.fødselsnummerBarn!!, it.behandlingId, 1, 1) }.toSet()

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify(exactly = 7) { taskService.save(any()) }
    }

    @Test
    fun `barn med bare termindato fyller ett år samme dag, forvent kall til beskrivelseBarnFyllerEttÅr og ingen unntak`() {
        val termindato = LocalDate.now().minusYears(1)
        val fødselsnummerSøker = FnrGenerator.generer(1992)
        val fødselsnummerBarn = FnrGenerator.generer(termindato)
        every { grunnlagsdataDomene.barn } returns listOf(barnMedIdent(fødselsnummerBarn, "Fornavn etternavn", fødsel(termindato)))

        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns
            listOf(
                opprettBarn(
                    fødselsnummer = null,
                    termindato = termindato,
                    fødselsnummerSøker = fødselsnummerSøker,
                ),
            )

        every { personService.hentPersonForelderBarnRelasjon(listOf(fødselsnummerSøker)) } returns
            mapOf(
                Pair(
                    fødselsnummerSøker,
                    PdlTestdataHelper.pdlBarn(
                        fødsel = PdlTestdataHelper.fødsel(fødselsdato = LocalDate.of(1992, 1, 1)),
                        forelderBarnRelasjon =
                            listOf(
                                ForelderBarnRelasjon(
                                    fødselsnummerBarn,
                                    Familierelasjonsrolle.BARN,
                                    Familierelasjonsrolle.MOR,
                                ),
                            ),
                    ),
                ),
            )

        every { personService.hentPersonForelderBarnRelasjon(listOf(fødselsnummerBarn)) } returns
            mapOf(
                Pair(
                    fødselsnummerBarn,
                    PdlTestdataHelper.pdlBarn(
                        fødsel = PdlTestdataHelper.fødsel(fødselsdato = termindato),
                        forelderBarnRelasjon = listOf(),
                    ),
                ),
            )

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        val opprettOppgavePayload = jsonMapper.readValue<OpprettOppgavePayload>(taskSlot.captured.payload)
        assertThat(opprettOppgavePayload.alder).isEqualTo(AktivitetspliktigAlder.ETT_ÅR)
    }

    @Test
    fun `et eldre barn i grunnlagsdata, forvent ingen opprettelser av oppgaver`() {
        val termindato = LocalDate.now().minusYears(10)
        val fødselsnummerSøker = FnrGenerator.generer(1992)
        val fødselsnummerBarn = FnrGenerator.generer(termindato)
        every { grunnlagsdataDomene.barn } returns listOf(barnMedIdent(fødselsnummerBarn, "Fornavn etternavn", fødsel(termindato)))

        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns
            listOf(
                opprettBarn(
                    fødselsnummer = fødselsnummerBarn,
                    termindato = termindato,
                    fødselsnummerSøker = fødselsnummerSøker,
                ),
            )

        every { personService.hentPersonForelderBarnRelasjon(listOf(fødselsnummerSøker)) } returns
            mapOf(
                Pair(
                    fødselsnummerSøker,
                    PdlTestdataHelper.pdlBarn(
                        fødsel = PdlTestdataHelper.fødsel(fødselsdato = LocalDate.of(1992, 1, 1)),
                        forelderBarnRelasjon =
                            listOf(
                                ForelderBarnRelasjon(
                                    fødselsnummerBarn,
                                    Familierelasjonsrolle.BARN,
                                    Familierelasjonsrolle.MOR,
                                ),
                            ),
                    ),
                ),
            )

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `to barn som fyller år på samme behandling, forvent at bare en oppgave er gjeldende grunnlagsdatabarn`() {
        val termindato = LocalDate.now().minusYears(1).minusDays(5)
        val behandlingId = UUID.randomUUID()
        val fødselsnummerSøker = FnrGenerator.generer()
        val fødselsnummerBarn = FnrGenerator.generer()
        val fødselsnummerBarn2 = FnrGenerator.generer()
        every { grunnlagsdataDomene.barn } returns
            listOf(
                barnMedIdent(fødselsnummerBarn, "Fornavn etternavn", fødsel(termindato)),
                barnMedIdent(fødselsnummerBarn2, "Fornavn etternavn", fødsel(termindato)),
            )

        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns
            listOf(
                opprettBarn(
                    behandlingId = behandlingId,
                    fødselsnummer = fødselsnummerBarn,
                    termindato = termindato,
                    fødselsnummerSøker = fødselsnummerSøker,
                ),
                opprettBarn(
                    behandlingId = behandlingId,
                    fødselsnummer = fødselsnummerBarn2,
                    termindato = termindato,
                    fødselsnummerSøker = fødselsnummerSøker,
                ),
            )

        every { personService.hentPersonForelderBarnRelasjon(listOf(fødselsnummerSøker)) } returns
            mapOf(
                Pair(
                    fødselsnummerSøker,
                    PdlTestdataHelper.pdlBarn(
                        fødsel = PdlTestdataHelper.fødsel(fødselsdato = termindato),
                        forelderBarnRelasjon =
                            listOf(
                                ForelderBarnRelasjon(
                                    fødselsnummerBarn,
                                    Familierelasjonsrolle.BARN,
                                    Familierelasjonsrolle.MOR,
                                ),
                                ForelderBarnRelasjon(
                                    fødselsnummerBarn2,
                                    Familierelasjonsrolle.BARN,
                                    Familierelasjonsrolle.MOR,
                                ),
                            ),
                    ),
                ),
            )

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()

        verify(exactly = 1) { taskService.save(any()) }
        val opprettOppgavePayload = jsonMapper.readValue<OpprettOppgavePayload>(taskSlot.captured.payload)
        assertThat(opprettOppgavePayload.alder).isEqualTo(AktivitetspliktigAlder.ETT_ÅR)
    }

    @Test
    fun `barn i rett aldre filtreres bort fordi det finnes oppgave fra før`() {
        val termindato = LocalDate.now().minusYears(1).minusDays(5)
        val behandlingId = UUID.randomUUID()
        val fødselsnummerSøker = FnrGenerator.generer()
        val fødselsnummerBarn = FnrGenerator.generer()

        val oppgave = oppgave(behandling = behandling()).copy(barnPersonIdent = fødselsnummerBarn, alder = AktivitetspliktigAlder.ETT_ÅR)
        every { oppgaveRepository.findByTypeAndAlderIsNotNullAndBarnPersonIdenter(any(), any()) } returns listOf(oppgave)

        every { grunnlagsdataDomene.barn } returns
            listOf(
                barnMedIdent(fødselsnummerBarn, "Fornavn etternavn", fødsel(termindato)),
            )

        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns
            listOf(
                opprettBarn(
                    behandlingId = behandlingId,
                    fødselsnummer = fødselsnummerBarn,
                    termindato = termindato,
                    fødselsnummerSøker = fødselsnummerSøker,
                ),
            )

        every { personService.hentPersonForelderBarnRelasjon(listOf(fødselsnummerSøker)) } returns
            mapOf(
                Pair(
                    fødselsnummerSøker,
                    PdlTestdataHelper.pdlBarn(
                        fødsel = PdlTestdataHelper.fødsel(fødselsdato = termindato),
                        forelderBarnRelasjon =
                            listOf(
                                ForelderBarnRelasjon(
                                    fødselsnummerBarn,
                                    Familierelasjonsrolle.BARN,
                                    Familierelasjonsrolle.MOR,
                                ),
                            ),
                    ),
                ),
            )

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()

        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test
    fun `terminbarn som fyller år på behandling, forvent at det oppdateres med data fra pdl`() {
        val termindato = LocalDate.now().minusYears(1).minusDays(5)
        val behandlingId = UUID.randomUUID()
        val fødselsnummerSøker = FnrGenerator.generer()
        val fødselsnummerBarn = FnrGenerator.generer()

        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns
            listOf(
                opprettBarn(
                    behandlingId = behandlingId,
                    fødselsnummer = null,
                    termindato = termindato,
                    fødselsnummerSøker = fødselsnummerSøker,
                ),
            )

        every { personService.hentPersonForelderBarnRelasjon(listOf(fødselsnummerSøker)) } returns
            mapOf(
                Pair(
                    fødselsnummerSøker,
                    PdlTestdataHelper.pdlPerson(
                        fødsel = PdlTestdataHelper.fødsel(fødselsdato = LocalDate.now().minusYears(30)),
                        forelderBarnRelasjon =
                            listOf(
                                ForelderBarnRelasjon(
                                    relatertPersonsIdent = fødselsnummerBarn,
                                    relatertPersonsRolle = Familierelasjonsrolle.BARN,
                                    minRolleForPerson = Familierelasjonsrolle.MOR,
                                ),
                            ),
                    ),
                ),
            )

        every { personService.hentPersonForelderBarnRelasjon(listOf(fødselsnummerBarn)) } returns
            mapOf(
                Pair(
                    fødselsnummerBarn,
                    PdlTestdataHelper.pdlPerson(
                        fødsel = PdlTestdataHelper.fødsel(fødselsdato = termindato),
                        forelderBarnRelasjon =
                            listOf(
                                ForelderBarnRelasjon(
                                    relatertPersonsIdent = fødselsnummerSøker,
                                    relatertPersonsRolle = Familierelasjonsrolle.MOR,
                                    minRolleForPerson = Familierelasjonsrolle.BARN,
                                ),
                            ),
                    ),
                ),
            )

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()

        verify(exactly = 1) { taskService.save(any()) }
        val opprettOppgavePayload = jsonMapper.readValue<OpprettOppgavePayload>(taskSlot.captured.payload)
        assertThat(opprettOppgavePayload.alder).isEqualTo(AktivitetspliktigAlder.ETT_ÅR)
    }

    @Test
    fun `to barn som fyller år på forskjellige behandlinger, forvent at to oppgaver er gjeldende`() {
        val fødselsdato = LocalDate.now().minusYears(1).minusDays(6)
        val fødselsnummere = listOf(FnrGenerator.generer(), FnrGenerator.generer())
        val opprettBarnForFødselsdatoer =
            listOf(
                opprettBarn(behandlingId = UUID.randomUUID(), fødselsnummer = fødselsnummere[0]),
                opprettBarn(behandlingId = UUID.randomUUID(), fødselsnummer = fødselsnummere[1]),
            )
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns opprettBarnForFødselsdatoer
        every { grunnlagsdataDomene.barn } returns fødselsnummere.mapIndexed { i, it -> barnMedIdent(it, "fornavn etternavn", fødsel(fødselsdato)) }

        opprettBarnForFødselsdatoer.map { BarnTilOppgave(it.fødselsnummerBarn!!, it.behandlingId, 1, 1) }.toSet()

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify(exactly = 2) { taskService.save(any()) }
    }

    @Test
    fun `barn fra vanlige behandlinger og migrerte fagsaker blir med i listen over oppgaver`() {
        val fødselsnummerBarn = FnrGenerator.generer(LocalDate.now().minusYears(1).minusDays(2))
        val fødselsnummerBarnMigrert = FnrGenerator.generer()
        val fødselsnummerSøker = FnrGenerator.generer()
        val behandlingId = UUID.randomUUID()
        val migrertBehandlingId = UUID.randomUUID()
        every { grunnlagsdataDomene.barn } returns
            listOf(
                barnMedIdent(fødselsnummerBarn, "Fornavn etternavn", fødsel(LocalDate.now().minusYears(1))),
                barnMedIdent(fødselsnummerBarnMigrert, "Fornavn etternavn", fødsel(LocalDate.now().minusYears(1))),
            )
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns
            listOf(
                opprettBarn(
                    behandlingId = behandlingId,
                    fødselsnummer = fødselsnummerBarn,
                    fødselsnummerSøker = fødselsnummerSøker,
                ),
            )

        every {
            gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns
            listOf(
                opprettBarn(
                    behandlingId = migrertBehandlingId,
                    fødselsnummer = fødselsnummerBarnMigrert,
                    fraMigrering = true,
                ),
            )

        opprettOppgaveForBarnService.opprettTasksForAlleBarnSomHarFyltÅr()
        verify(exactly = 2) { taskService.save(any()) }
    }

    private fun opprettBarn(
        behandlingId: UUID = UUID.randomUUID(),
        fødselsnummerSøker: String = "12345678910",
        fødselsnummer: String? = null,
        termindato: LocalDate? = null,
        fraMigrering: Boolean = false,
    ): BarnTilUtplukkForOppgave = BarnTilUtplukkForOppgave(behandlingId, fødselsnummerSøker, fødselsnummer, termindato, fraMigrering)
}
