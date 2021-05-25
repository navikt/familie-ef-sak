package no.nav.familie.ef.sak.api.ekstern

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdent
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdenter
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.kontrakter.felles.PersonIdent
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
    internal fun `skal returnere false når det ikke finnes en behandling`() {
        every { behandlingRepository.finnSisteBehandling(Stønadstype.OVERGANGSSTØNAD, setOf("1", "2")) } returns null
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(Stønadstype.OVERGANGSSTØNAD, PersonIdent("1")).data)
                .isEqualTo(false)
    }

    @Test
    internal fun `skal returnere false når behandlingen er av type blankett`() {
        every { behandlingRepository.finnSisteBehandling(Stønadstype.OVERGANGSSTØNAD, setOf("1", "2")) } returns
                behandling(fagsak(), type = BehandlingType.BLANKETT)
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(Stønadstype.OVERGANGSSTØNAD, PersonIdent("1")).data)
                .isEqualTo(false)
    }

    @Test
    internal fun `skal returnere false når resultat er annulert`() {
        every { behandlingRepository.finnSisteBehandling(Stønadstype.OVERGANGSSTØNAD, setOf("1", "2")) } returns
                behandling(fagsak(), resultat = BehandlingResultat.ANNULLERT)
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(Stønadstype.OVERGANGSSTØNAD, PersonIdent("1")).data)
                .isEqualTo(false)
    }

    @Test
    internal fun `skal returnere true når behandling finnes`() {
        every { behandlingRepository.finnSisteBehandling(Stønadstype.OVERGANGSSTØNAD, setOf("1", "2")) } returns
                behandling(fagsak(), type = BehandlingType.FØRSTEGANGSBEHANDLING, resultat = BehandlingResultat.IKKE_SATT)
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(Stønadstype.OVERGANGSSTØNAD, PersonIdent("1")).data)
                .isEqualTo(true)
    }
}