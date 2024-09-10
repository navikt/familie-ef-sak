package no.nav.familie.ef.sak.behandling.migrering

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.felles.util.opprettBarnMedIdent
import no.nav.familie.ef.sak.felles.util.opprettGrunnlagsdata
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.OpprettOppgavePayload
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnMinimumDto
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class OpprettOppgaveForMigrertFødtBarnTaskTest {
    val behandlingService = mockk<BehandlingService>()
    val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    val grunnlagsdataService = mockk<GrunnlagsdataService>()
    val taskService = mockk<TaskService>()
    val service =
        OpprettOppgaveForMigrertFødtBarnTask(
            behandlingService,
            tilkjentYtelseService,
            grunnlagsdataService,
            taskService,
        )

    val taskSlot = slot<List<Task>>()

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentAktivIdent(any()) } returns "1"
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling(fagsak())
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse(LocalDate.now().plusYears(2))
        every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns opprettGrunnlagsdata(null)
        every { taskService.saveAll(capture(taskSlot)) } answers { firstArg() }
    }

    @Test
    internal fun `skal ikke opprette oppgave hvis fødelsdato mangler`() {
        service.doTask(opprettOppgave(null))

        verify(exactly = 0) { taskService.saveAll(any()) }
    }

    @Test
    internal fun `skal ikke opprette oppgave hvis barn allerede finnes i grunnlagsdata`() {
        val fødelsdato = LocalDate.now().minusDays(3)
        every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns opprettGrunnlagsdata(fødelsdato)

        service.doTask(opprettOppgave(fødelsdato))

        verify(exactly = 0) { taskService.saveAll(any()) }
    }

    @Test
    internal fun `skal ikke opprette oppgave hvis siste utbetalingsperioden er før barnet fyller 1 år`() {
        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelse(LocalDate.now().plusMonths(10))

        service.doTask(opprettOppgave(LocalDate.now()))
        verify(exactly = 0) { taskService.saveAll(any()) }
    }

    @Test
    internal fun `skal opprette 2 oppgaver for nytt barn`() {
        val halvtÅr = LocalDate.now().plusMonths(6)
        val etÅr = LocalDate.now().plusYears(1)

        service.doTask(opprettOppgave(LocalDate.now()))

        val tasks = taskSlot.captured
        assertThat(tasks.size).isEqualTo(2)

        assertThat(
            tasks.single {
                objectMapper.readValue<OpprettOppgavePayload>(it.payload).aktivFra in
                    halvtÅr.minusWeeks(1)..halvtÅr.plusWeeks(2)
            },
        )
        assertThat(
            tasks.single {
                objectMapper.readValue<OpprettOppgavePayload>(it.payload).aktivFra in
                    etÅr.minusWeeks(1)..etÅr.plusWeeks(2)
            },
        )
    }

    private fun tilkjentYtelse(tilOgMed: LocalDate) =
        lagTilkjentYtelse(
            listOf(
                lagAndelTilkjentYtelse(1, fraOgMed = LocalDate.now(), tilOgMed = tilOgMed),
            ),
        )

    private fun opprettGrunnlagsdata(barnFødelsdato: LocalDate?): GrunnlagsdataMedMetadata {
        val grunnlagsdata = opprettGrunnlagsdata()
        val fødsel =
            Fødsel(
                foedselsdato = barnFødelsdato,
                foedekommune = null,
                foedeland = null,
                foedested = null,
                foedselsaar = null,
            )
        val barn = opprettBarnMedIdent("1", fødsel = fødsel)
        return GrunnlagsdataMedMetadata(grunnlagsdata.copy(barn = listOf(barn)), LocalDateTime.now())
    }

    private fun opprettOppgave(fødelsdato: LocalDate?) =
        OpprettOppgaveForMigrertFødtBarnTask.opprettOppgave(
            fagsak(fagsakpersoner(setOf("1"))),
            listOf(BarnMinimumDto("1", "", fødelsdato)),
        )
}
