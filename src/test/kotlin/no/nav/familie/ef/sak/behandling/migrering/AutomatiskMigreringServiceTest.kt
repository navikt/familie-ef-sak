package no.nav.familie.ef.sak.behandling.migrering

import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

internal class AutomatiskMigreringServiceTest {

    private val migreringsstatusRepository = mockk<MigreringsstatusRepository>()
    private val infotrygdReplikaClient = mockk<InfotrygdReplikaClient>()
    private val migreringService = mockk<MigreringService>()
    private val taskRepository = mockk<TaskRepository>()

    private val service = AutomatiskMigreringService(migreringsstatusRepository,
                                                     migreringService,
                                                     infotrygdReplikaClient,
                                                     taskRepository)

    private val updateSlots = mutableListOf<Migreringsstatus>()
    private val insertAllSlot = slot<List<Migreringsstatus>>()

    @BeforeEach
    internal fun setUp() {
        updateSlots.clear()
        clearMocks(migreringsstatusRepository, infotrygdReplikaClient, migreringService)
        every { migreringService.migrerOvergangsstønadAutomatisk(any()) } just Runs
        every { migreringsstatusRepository.update(capture(updateSlots)) } answers { firstArg() }
        mockFindById(MigreringResultat.IKKE_KONTROLLERT)
        every { migreringsstatusRepository.findAllByIdentIn(any()) } returns emptySet()
        every { migreringsstatusRepository.insertAll(capture(insertAllSlot)) } answers { firstArg() }
        every { taskRepository.saveAll(any<Iterable<Task>>()) } answers { firstArg() }
    }

    @Test
    internal fun `har allerede migrert en av identene, hopper over den`() {
        every { infotrygdReplikaClient.hentPersonerForMigrering(any()) } returns setOf("1", "2", "3", "4", "5")
        every { migreringsstatusRepository.findAllByIdentIn(any()) } returns
                setOf(Migreringsstatus("2", MigreringResultat.FEILET))

        service.migrerAutomatisk(3)
        assertThat(insertAllSlot.captured).hasSize(3)
        assertThat(insertAllSlot.captured.map { it.ident }).containsExactlyInAnyOrder("1", "3", "4")
        assertThat(insertAllSlot.captured.map { it.status }).containsOnly(MigreringResultat.IKKE_KONTROLLERT)
    }

    @Test
    internal fun `migrer person automatisk`() {
        val ident = "1"
        service.migrerPersonAutomatisk(ident)
        assertThat(updateSlots).hasSize(1)
        assertThat(updateSlots[0].ident).isEqualTo(ident)
        assertThat(updateSlots[0].status).isEqualTo(MigreringResultat.OK)
    }

    @Test
    internal fun `feiler migrering, legger inn feilet i databasen`() {
        val ident = "1"
        val migreringException = MigreringException("Feilet", MigreringExceptionType.ALLEREDE_MIGRERT)
        every { migreringService.migrerOvergangsstønadAutomatisk(any()) } throws migreringException

        service.migrerPersonAutomatisk(ident)
        assertThat(updateSlots).hasSize(1)
        assertThat(updateSlots[0].ident).isEqualTo(ident)
        assertThat(updateSlots[0].status).isEqualTo(MigreringResultat.FEILET)
        assertThat(updateSlots[0].årsak).isEqualTo(MigreringExceptionType.ALLEREDE_MIGRERT)
    }

    @Test
    internal fun `har allerede migrert person`() {
        val ident = "1"
        mockFindById(MigreringResultat.OK)
        service.migrerPersonAutomatisk(ident)
        assertThat(updateSlots).isEmpty()
        verify(exactly = 0) { migreringService.migrerOvergangsstønadAutomatisk(any()) }
    }

    private fun mockFindById(migreringResultat: MigreringResultat) {
        every { migreringsstatusRepository.findById(any()) } answers {
            Optional.of(Migreringsstatus(firstArg(), status = migreringResultat))
        }
    }
}