package no.nav.familie.ef.sak.behandling.migrering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import no.nav.familie.ef.sak.infotrygd.InfotrygdPerioderDto
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infotrygd.InfotrygdStønadPerioderDto
import no.nav.familie.ef.sak.infotrygd.tilSummertInfotrygdperiodeDto
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class InfotrygdPeriodeValideringServiceTest {

    private val infotrygdService = mockk<InfotrygdService>()
    private val service = InfotrygdPeriodeValideringService(infotrygdService)

    private val personIdent = "1"

    @BeforeEach
    internal fun setUp() {
        every { infotrygdService.eksisterer(any(), any()) } returns true
        every { infotrygdService.hentSaker(any()) } returns InfotrygdSakResponse(emptyList())
    }

    @Nested
    inner class ValiderKanJournalføreUtenÅMigrere {

        @Test
        internal fun `skal kunne journalføre når personen ikke har noen saker i infotrygd`() {
            every { infotrygdService.hentDtoPerioder(personIdent) } returns infotrygdPerioderDto(emptyList())

            service.validerKanJournalføreUtenÅMigrere(personIdent, StønadType.OVERGANGSSTØNAD)
        }

        @Test
        internal fun `skal kunne journalføre når det ikke trengs migrering - når personen har perioder langt bak i tiden`() {
            val dato = YearMonth.now().minusYears(4)
            every { infotrygdService.hentDtoPerioder(personIdent) } returns
                    infotrygdPerioderDto(listOf(lagInfotrygdPeriode(personIdent = "1",
                                                                    stønadFom = dato.atDay(1),
                                                                    stønadTom = dato.atEndOfMonth())))
            service.validerKanJournalføreUtenÅMigrere(personIdent, StønadType.OVERGANGSSTØNAD)
        }

        @Test
        internal fun `kan ikke journalføre når personen har periode`() {
            every { infotrygdService.hentDtoPerioder(personIdent) } returns
                    infotrygdPerioderDto(listOf(lagInfotrygdPeriode()))

            assertThatThrownBy { service.validerKanJournalføreUtenÅMigrere(personIdent, StønadType.OVERGANGSSTØNAD) }
                    .isInstanceOf(ApiFeil::class.java)
        }

        @Test
        internal fun `kan ikke journalføre når det finnes flere identer på perioder i infotrygd`() {
            every { infotrygdService.hentDtoPerioder(personIdent) } returns
                    infotrygdPerioderDto(listOf(lagInfotrygdPeriode(personIdent = "1", vedtakId = 1),
                                                lagInfotrygdPeriode(personIdent = "2", vedtakId = 2)))

            assertThatThrownBy { service.validerKanJournalføreUtenÅMigrere(personIdent, StønadType.OVERGANGSSTØNAD) }
                    .isInstanceOf(ApiFeil::class.java)
        }
    }

    private fun infotrygdPerioderDto(perioder: List<InfotrygdPeriode> = emptyList()): InfotrygdPerioderDto {
        val summertePerioder = perioder.map { it.tilSummertInfotrygdperiodeDto() }
        val emptyStønadPeriodeDto = InfotrygdStønadPerioderDto(perioder, summertePerioder)
        return InfotrygdPerioderDto(emptyStønadPeriodeDto, emptyStønadPeriodeDto, emptyStønadPeriodeDto)
    }
}