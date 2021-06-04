package no.nav.familie.ef.sak.api.ekstern

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdent
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EksternBehandlingControllerTest {

    private val pdlClient = mockk<PdlClient>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val eksternBehandlingController = EksternBehandlingController(pdlClient, behandlingRepository)

    @BeforeEach
    internal fun setUp() {
        every { pdlClient.hentPersonidenter("1", true) } returns PdlIdenter(listOf(PdlIdent("1", true), PdlIdent("2", false)))
    }

    @Test
    internal fun `skal feile når den ikke finner identer til personen`() {
        every { pdlClient.hentPersonidenter("1", true) } returns PdlIdenter(emptyList())
        val finnesBehandlingForPerson =
                eksternBehandlingController.finnesBehandlingForPerson(Stønadstype.OVERGANGSSTØNAD, PersonIdent("1"))
        assertThat(finnesBehandlingForPerson.status)
                .isEqualTo(Ressurs.Status.FEILET)
    }

    @Test
    internal fun `skal returnere false når det ikke finnes en behandling`() {
        every {
            behandlingRepository.eksistererBehandlingSomIkkeErBlankett(Stønadstype.OVERGANGSSTØNAD, setOf("1", "2"))
        } returns false
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(Stønadstype.OVERGANGSSTØNAD, PersonIdent("1")).data)
                .isEqualTo(false)
    }

    @Test
    internal fun `skal returnere true når behandling finnes`() {
        every {
            behandlingRepository.eksistererBehandlingSomIkkeErBlankett(Stønadstype.OVERGANGSSTØNAD, setOf("1", "2"))
        } returns true
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(Stønadstype.OVERGANGSSTØNAD, PersonIdent("1")).data)
                .isEqualTo(true)
    }

    @Test
    internal fun `uten stønadstype - skal returnere false når det ikke finnes noen behandling`() {
        every { behandlingRepository.eksistererBehandlingSomIkkeErBlankett(any(), setOf("1", "2")) } returns false
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(null, PersonIdent("1")).data)
                .isEqualTo(false)
    }

    @Test
    internal fun `uten stønadstype - skal returnere true når det minimum en behandling`() {
        var counter = 0
        every { behandlingRepository.eksistererBehandlingSomIkkeErBlankett(any(), setOf("1", "2")) } answers { counter++ == 1 }
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(null, PersonIdent("1")).data)
                .isEqualTo(true)
        verify(exactly = 2) { behandlingRepository.eksistererBehandlingSomIkkeErBlankett(any(), any()) }
    }
}