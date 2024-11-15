package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonForelderBarn
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødsel
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.pdlBarn
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForBarn
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class ForberedOppgaverTerminbarnServiceTest {
    private val personService: PersonService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val terminbarnRepository: TerminbarnRepository = mockk()
    private val taskService: TaskService = mockk()
    private val forberedOppgaverTerminbarnService =
        ForberedOppgaverTerminbarnService(personService, fagsakService, terminbarnRepository, taskService)
    val fødsel: Fødsel = mockk()

    @BeforeEach
    fun init() {
        every { fagsakService.hentAktivIdent(any()) } returns ""
        every { personService.hentPersonMedBarn(any()).barn } returns mockk()
        every { terminbarnRepository.insert(any()) } returns mockk()
        mockkObject(GrunnlagsdataMapper)
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(GrunnlagsdataMapper)
    }

    @Test
    fun `ett utløpt terminbarn som ikke finnes i terminbarnRepo, ingen barn i PDL, forvent at oppgave lagres og opprettes`() {
        val terminBarn = listOf(opprettTerminbarn())
        val pdlPersonForelderBarn = emptyList<PdlPersonForelderBarn>()
        every { personService.hentPersonMedBarn(any()).barn.values } returns pdlPersonForelderBarn
        every { terminbarnRepository.finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn(StønadType.OVERGANGSSTØNAD) } returns terminBarn
        every { taskService.save(any()) } returns mockk()

        forberedOppgaverTerminbarnService.forberedOppgaverForUfødteTerminbarn()
        verify(exactly = 1) { taskService.save(any()) }
        verify(exactly = 1) { terminbarnRepository.insert(any()) }
    }

    @Test
    fun `ett utløpt terminbarn som ikke finnes i terminbarnRepo, ingen barn i PDL, forvent at oppgave instansieres riktig`() {
        val terminbarn = listOf(opprettTerminbarn(UUID.randomUUID(), UUID.randomUUID(), 1, LocalDate.MIN))
        val pdlPersonForelderBarn = emptyList<PdlPersonForelderBarn>()
        val oppgaverForBarnSlot = slot<Task>()
        every { personService.hentPersonMedBarn(any()).barn.values } returns pdlPersonForelderBarn
        every { terminbarnRepository.finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn(StønadType.OVERGANGSSTØNAD) } returns terminbarn
        every { taskService.save(capture(oppgaverForBarnSlot)) } returns mockk()

        forberedOppgaverTerminbarnService.forberedOppgaverForUfødteTerminbarn()
        val capture = objectMapper.readValue<OppgaveForBarn>(oppgaverForBarnSlot.captured.payload)
        assertThat(capture.behandlingId).isEqualTo(terminbarn.first().behandlingId)
        assertThat(capture.eksternFagsakId).isEqualTo(terminbarn.first().eksternFagsakId)
        assertThat(capture.beskrivelse).isEqualTo(OppgaveBeskrivelse.beskrivelseUfødtTerminbarn())
    }

    @Test
    fun `ett utløpt terminbarn som ikke finnes i terminbarnRepo, to umatchede barn i PDL, forvent at oppgave opprettes`() {
        val terminBarn = listOf(opprettTerminbarn())
        val pdlBarn =
            listOf(
                opprettPdlBarn(fødselsdato = LocalDate.now().minusYears(1)),
                opprettPdlBarn(fødselsdato = LocalDate.now().minusYears(2)),
            )

        every { personService.hentPersonMedBarn(any()).barn.values } returns pdlBarn
        every { terminbarnRepository.finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn(StønadType.OVERGANGSSTØNAD) } returns terminBarn
        every { taskService.save(any()) } returns mockk()

        forberedOppgaverTerminbarnService.forberedOppgaverForUfødteTerminbarn()
        verify(exactly = 1) { taskService.save(any()) }
        verify(exactly = 1) { terminbarnRepository.insert(any()) }
    }

    @Test
    fun `ett utløpt terminbarn, ett av to matchede PDL barn, forvent at oppgave ikke opprettes`() {
        val terminBarn = listOf(opprettTerminbarn())
        val pdlBarn =
            listOf(
                opprettPdlBarn(fødselsdato = LocalDate.now().plusWeeks(3)),
                opprettPdlBarn(fødselsdato = LocalDate.now().minusYears(2)),
            )
        every { personService.hentPersonMedBarn(any()).barn.values } returns pdlBarn
        every { terminbarnRepository.finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn(StønadType.OVERGANGSSTØNAD) } returns terminBarn
        every { taskService.save(any()) } returns mockk()

        forberedOppgaverTerminbarnService.forberedOppgaverForUfødteTerminbarn()
        verify(exactly = 0) { taskService.save(any()) }
        verify(exactly = 0) { terminbarnRepository.insert(any()) }
    }

    @Test
    fun `ingen terminbarn finnes, to PDL barn finnes, forvent at oppgave ikke opprettes`() {
        val terminBarn = emptyList<TerminbarnTilUtplukkForOppgave>()
        val pdlBarn =
            listOf(
                opprettPdlBarn(fødselsdato = LocalDate.now().plusWeeks(3)),
                opprettPdlBarn(fødselsdato = LocalDate.now().minusYears(2)),
            )
        every { personService.hentPersonMedBarn(any()).barn.values } returns pdlBarn
        every { terminbarnRepository.finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn(StønadType.OVERGANGSSTØNAD) } returns terminBarn
        every { taskService.save(any()) } returns mockk()

        forberedOppgaverTerminbarnService.forberedOppgaverForUfødteTerminbarn()
        verify(exactly = 0) { taskService.save(any()) }
    }

    private fun opprettTerminbarn(
        behandlingId: UUID = UUID.randomUUID(),
        fagsakId: UUID = UUID.randomUUID(),
        eksternId: Long = 0,
        termindato: LocalDate = LocalDate.now(),
    ): TerminbarnTilUtplukkForOppgave = TerminbarnTilUtplukkForOppgave(behandlingId, fagsakId, eksternId, termindato)

    private fun opprettPdlBarn(fødselsdato: LocalDate): PdlPersonForelderBarn = pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato))
}
