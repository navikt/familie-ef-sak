package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.dto.EksternId
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.iverksett.OppgaverForBarnDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

/**
 * Vi tar utgangspunkt i at en periode er [fra, til], dvs fra og med frem til og med, ved sjekk om en fødselsdato slår til i løpet
 * av k antall uker (antakeligvis alltid bare én uke).
 *
 * En kjøring som er for tidlig ute skal få f.o.m i dag, og en som kjøres for sent skal få en f.o.m dato som er den første dagen
 * etter den forrige perioden.
 */
internal class ForberedOppgaverForBarnServiceTest {

    private val gjeldendeBarnRepository = mockk<GjeldendeBarnRepository>()
    private val iverksettClient = mockk<IverksettClient>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val opprettOppgaveForBarnService =
        ForberedOppgaverForBarnService(gjeldendeBarnRepository, behandlingRepository, iverksettClient)

    private val REFERANSEDATO = LocalDate.now()

    private val oppgaveSlot = slot<OppgaverForBarnDto>()
    private val eksterneIderSlot = slot<Set<UUID>>()

    @BeforeEach
    fun init() {
        oppgaveSlot.clear()
        eksterneIderSlot.clear()
        mockkObject(OppgaveBeskrivelse)
        every { iverksettClient.sendOppgaverForBarn(capture(oppgaveSlot)) } just runs
        every { gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(any(), any()) } returns emptyList()
        every { behandlingRepository.finnEksterneIder(capture(eksterneIderSlot)) } answers {
            firstArg<Set<UUID>>()
                .mapIndexed { index, behandlingId -> EksternId(behandlingId, index.toLong(), index.toLong()) }.toSet()
        }
    }

    @AfterEach
    internal fun tearDown() {
        unmockkObject(OppgaveBeskrivelse)
    }

    @Test
    fun `barn blir seks mnd om 4 dager, sjekk om fyller innen 1 uke, forvent kall til beskrivelseBarnBlirSeksMnd`() {
        val fødselsdato = LocalDate.now().minusDays(182).plusDays(3)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir seks mnd i dag, sjekk om fyller innen 1 uke, forvent kall til beskrivelseBarnBlirSeksMnd`() {
        val fødselsdato = LocalDate.now().minusDays(182)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify(exactly = 1) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir seks mnd om 1 uke minus en dag, sjekk om fyller innen 1 uke, forvent kall til beskrivelseBarnBlirSeksMnd`() {
        val fødselsdato = LocalDate.now().minusDays(182).plusWeeks(1).minusDays(1)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir seks mnd om 7 dager, sjekk om fyller innen 1 uke, forvent 0 beskrivelseBarnBlirSeksMnd`() {
        val fødselsdato = LocalDate.now().minusDays(182).plusDays(7)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir seks mnd om 6 dager, sjekk om fyller innen 1 uke, forvent beskrivelseBarnBlirSeksMnd`() {
        val fødselsdato = LocalDate.now().minusDays(182).plusDays(6)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify(exactly = 1) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir seks mnd om 1 uker pluss en dag, sjekk om fyller innen 1 uke, forvent ingen kall`() {
        val fødselsdato = LocalDate.now().minusDays(182).plusWeeks(1).plusDays(1)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn født 29 august skal ikke få opprettet ny oppgave ved kjøring 21 februar`() {
        val fødselsdato = LocalDate.of(2022, 8, 29)
        val kjøreDato = LocalDate.of(2022, 2, 21)
        val barn = opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato))
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(barn)
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(kjøreDato)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
    }

    @Test
    fun `7 av 16 barn blir 6 mnd innen 1 uke, sjekk fyller innen 1 uke, forvent 8 kall til beskrivelseBarnBlirSeksMnd`() {
        val fødselsdatoer = (-1..14).asSequence().map { LocalDate.now().minusDays(182).plusDays(it.toLong()) }.toList()
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns fødselsdatoer.map { opprettBarn(fødselsnummer = FnrGenerator.generer(it)) }
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
        verify(exactly = 7) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
    }

    @Test
    fun `7 av 16 barn blir 1 år innen 1 uke, sjekk fyller innen 1 uke, forvent 8 kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdatoer = (-1..14).asSequence().map { LocalDate.now().minusYears(1).plusDays(it.toLong()) }.toList()
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns fødselsdatoer.map { opprettBarn(fødselsnummer = FnrGenerator.generer(it)) }
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify(exactly = 7) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
    }

    @Test
    fun `barn blir 1 år om 4 dager, sjekk om fyller innen 1 uke, forvent kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdato = LocalDate.now().minusYears(1).plusDays(3)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir 1 år i dag, sjekk om fyller innen 1 uke, forvent kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdato = LocalDate.now().minusYears(1)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 1) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir 1 år om 6 dager, sjekk om fyller innen 1 uke, forvent 1 kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdato = LocalDate.now().minusYears(1).plusDays(6)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 1) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir 1 år om 7 dager, sjekk om fyller innen 1 uke, forvent 0 kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdato = LocalDate.now().minusYears(1).plusDays(7)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn ble 1 år for 3 dager siden, for sen kjøring med 2 dager, sjekk om fyller innen 1 uke, forvent ingen kall`() {
        val fødselsdato = LocalDate.now().minusYears(1).minusDays(3)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = FnrGenerator.generer(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO.minusDays(2))
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn med bare termindato fyller 1 år i morgen, forvent kall til beskrivelseBarnFyllerEttÅr og ingen unntak`() {
        val termindato = LocalDate.now().minusYears(1).plusDays(1)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = null, termindato = termindato))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 1) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `to barn som fyller år på samme behandling, forvent at bare en oppgave er gjeldende`() {
        val termindato = LocalDate.now().minusYears(1).plusDays(6)
        val behandlingId = UUID.randomUUID()

        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(
            opprettBarn(behandlingId = behandlingId, fødselsnummer = null, termindato = termindato),
            opprettBarn(behandlingId = behandlingId, fødselsnummer = null, termindato = termindato)
        )
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        assertThat(eksterneIderSlot.captured.size).isEqualTo(1)
    }

    @Test
    fun `to barn som fyller år på forskjellige behandlinger, forvent at to oppgaver er gjeldende`() {
        val termindato = LocalDate.now().minusYears(1).plusDays(6)

        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(
            opprettBarn(behandlingId = UUID.randomUUID(), fødselsnummer = null, termindato = termindato),
            opprettBarn(behandlingId = UUID.randomUUID(), fødselsnummer = null, termindato = termindato)
        )
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)
        assertThat(eksterneIderSlot.captured.size).isEqualTo(2)
    }

    @Test
    fun test() {
        val date = LocalDate.of(2021, 12, 2)
        val halvtÅr = date.plusDays(182)
        println(halvtÅr) // 02.06.2022
    }

    @Test
    fun `barn fra vanlige behandlinger og migrerte fagsaker blir med i listen over oppgaver`() {
        val termindato = LocalDate.now().minusYears(1).plusDays(6)
        val fødselsdato = LocalDate.now().minusDays(182)

        val behandlingId = UUID.randomUUID()
        val migrertBehandlingId = UUID.randomUUID()

        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(behandlingId = behandlingId, termindato = termindato))
        every {
            gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(StønadType.OVERGANGSSTØNAD, any())
        } returns listOf(
            opprettBarn(
                behandlingId = migrertBehandlingId,
                fødselsnummer = FnrGenerator.generer(fødselsdato),
                fraMigrering = true
            )
        )

        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(REFERANSEDATO)

        val oppgaverForBarn = oppgaveSlot.captured.oppgaverForBarn
        assertThat(oppgaverForBarn).hasSize(2)
        assertThat(oppgaverForBarn.map { it.behandlingId }).containsExactlyInAnyOrder(behandlingId, migrertBehandlingId)
    }

    private fun opprettBarn(
        behandlingId: UUID = UUID.randomUUID(),
        fødselsnummer: String? = null,
        termindato: LocalDate? = null,
        fraMigrering: Boolean = false
    ): BarnTilUtplukkForOppgave {
        return BarnTilUtplukkForOppgave(behandlingId, "12345678910", fødselsnummer, termindato, fraMigrering)
    }
}
