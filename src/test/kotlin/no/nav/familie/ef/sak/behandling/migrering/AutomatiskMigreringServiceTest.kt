package no.nav.familie.ef.sak.behandling.migrering

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AutomatiskMigreringServiceTest {

    private val migreringsstatusRepository = mockk<MigreringsstatusRepository>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>()
    private val migreringService = mockk<MigreringService>()

    private val service = AutomatiskMigreringService(migreringsstatusRepository, migreringService, infotrygdReplikaClient)

    private val slots = mutableListOf<Migreringsstatus>()

    @BeforeEach
    internal fun setUp() {
        slots.clear()
        clearMocks(migreringsstatusRepository, infotrygdReplikaClient, migreringService)
        every { migreringsstatusRepository.insert(capture(slots)) } answers { firstArg() }
        every { migreringsstatusRepository.findAllByIdentIn(any()) } returns emptySet()
    }

    @Test
    internal fun `har allerede migrert en av identene, hopper over den`() {
        every { infotrygdReplikaClient.hentPersonerForMigrering(any()) } returns setOf("1", "2", "3", "4", "5")
        every { migreringsstatusRepository.findAllByIdentIn(any()) } returns
                setOf(Migreringsstatus("2", MigreringResultat.FEILET))
        every { migreringService.migrerOvergangsstønadAutomatisk(any()) } just Runs

        service.migrerAutomatisk(3)
        verifyOrder {
            migreringService.migrerOvergangsstønadAutomatisk("1")
            // hopper over 2 som allerede finnes i migreringstabellen
            migreringService.migrerOvergangsstønadAutomatisk("3")
            migreringService.migrerOvergangsstønadAutomatisk("4")
        }
        assertThat(slots).hasSize(3)
        assertThat(slots.all { it.status == MigreringResultat.OK }).isTrue
    }

    @Test
    internal fun `feiler migrering, legger inn feilet i databasen`() {
        val migreringException = MigreringException("Feilet", MigreringExceptionType.ALLEREDE_MIGRERT)
        every { infotrygdReplikaClient.hentPersonerForMigrering(any()) } returns setOf("1")
        every { migreringService.migrerOvergangsstønadAutomatisk(any()) } throws migreringException

        service.migrerAutomatisk(3)
        verify(exactly = 1) { migreringService.migrerOvergangsstønadAutomatisk(any()) }
        assertThat(slots).hasSize(1)
        assertThat(slots.all { it.status == MigreringResultat.FEILET }).isTrue
    }
}