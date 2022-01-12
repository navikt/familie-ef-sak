package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.util.FnrGenerator
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
internal class ForberedOppgaveForBarnServiceTest {

    val gjeldendeBarnRepository = mockk<GjeldendeBarnRepository>()
    val iverksettClient = mockk<IverksettClient>()
    val opprettOppgaveForBarnService = ForberedOppgaveForBarnService(gjeldendeBarnRepository, iverksettClient)

    val SISTE_KJØRING_EN_UKE_SIDEN = LocalDate.now().minusWeeks(1)

    @BeforeEach
    fun init() {
        mockkObject(OppgaveBeskrivelse)
        every { iverksettClient.sendOppgaverForBarn(any()) } just runs
    }

    @Test
    fun `barn blir seks mnd om 4 dager, sjekk om fyller innen 1 uke, forvent kall til beskrivelseBarnBlirSeksMnd`() {
        val fødselsdato = LocalDate.now().minusMonths(6).plusDays(3)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = generateFnr(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN)
        verify { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir seks mnd i dag, sjekk om fyller innen 1 uke, forvent kall til beskrivelseBarnBlirSeksMnd`() {
        val fødselsdato = LocalDate.now().minusMonths(6)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = generateFnr(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN)
        verify(exactly = 1) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }


    @Test
    fun `barn blir seks mnd om 1 uke minus en dag, sjekk om fyller innen 1 uke, forvent kall til beskrivelseBarnBlirSeksMnd`() {
        val fødselsdato = LocalDate.now().minusMonths(6).plusWeeks(1).minusDays(1)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = generateFnr(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN)
        verify { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir seks mnd om 1 uke, sjekk om fyller innen 1 uke, forvent beskrivelseBarnBlirSeksMnd`() {
        val fødselsdato = LocalDate.now().minusMonths(6).plusWeeks(1)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = generateFnr(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN)
        verify { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir seks mnd om 1 uker pluss en dag, sjekk om fyller innen 1 uke, forvent ingen kall`() {
        val fødselsdato = LocalDate.now().minusMonths(6).plusWeeks(1).plusDays(1)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = generateFnr(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `8 av 14 barn blir 6 mnd innen 1 uke, sjekk fyller innen 1 uke, forvent 8 kall til beskrivelseBarnBlirSeksMnd`() {
        val fødselsdatoer = (0..14).asSequence().map { LocalDate.now().minusMonths(6).plusDays(it.toLong()) }.toList()
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns fødselsdatoer.map { opprettBarn(generateFnr(it)) }
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
        verify(exactly = 8) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
    }

    @Test
    fun `8 av 14 barn blir 1 år innen 1 uke, sjekk fyller innen 1 uke, forvent 8 kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdatoer = (0..14).asSequence().map { LocalDate.now().minusYears(1).plusDays(it.toLong()) }.toList()
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns fødselsdatoer.map { opprettBarn(generateFnr(it)) }
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN)
        verify(exactly = 8) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
    }

    @Test
    fun `barn blir 1 år om 4 dager, sjekk om fyller innen 1 uke, forvent kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdato = LocalDate.now().minusYears(1).plusDays(3)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = generateFnr(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir 1 år i dag, sjekk om fyller innen 1 uke, forvent kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdato = LocalDate.now().minusYears(1)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = generateFnr(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 1) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn blir 1 år om 7 dager, sjekk om fyller innen 1 uke, forvent kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdato = LocalDate.now().minusYears(1).plusDays(7)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = generateFnr(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN)
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn ble 1 år i går, for sen kjøring med 2 dager, sjekk om fyller innen 1 uke, forvent kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdato = LocalDate.now().minusYears(1).minusDays(1)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = generateFnr(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN.minusDays(2))
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `barn ble 1 år for 3 dager siden, for sen kjøring med 2 dager, sjekk om fyller innen 1 uke, forvent ingen kall`() {
        val fødselsdato = LocalDate.now().minusYears(1).minusDays(3)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = generateFnr(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN.minusDays(2))
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    @Test
    fun `for tidlig kjøring med 7 dager, barn fyller om 7 dager, forvent kall til beskrivelseBarnFyllerEttÅr`() {
        val fødselsdato = LocalDate.now().minusYears(1).plusDays(7)
        every {
            gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(Stønadstype.OVERGANGSSTØNAD, any())
        } returns listOf(opprettBarn(fødselsnummer = generateFnr(fødselsdato)))
        opprettOppgaveForBarnService.forberedOppgaverForAlleBarnSomFyllerAarNesteUke(SISTE_KJØRING_EN_UKE_SIDEN.minusDays(7))
        verify(exactly = 0) { OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd() }
        verify(exactly = 1) { OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr() }
    }

    private fun generateFnr(localDate: LocalDate): String {
        return FnrGenerator.generer(localDate.year, localDate.month.value, localDate.dayOfMonth, false)
    }

    private fun opprettBarn(fødselsnummer: String? = null, termindato: LocalDate? = null): GjeldendeBarn {
        return GjeldendeBarn(UUID.randomUUID(), null, fødselsnummer, termindato)
    }


}